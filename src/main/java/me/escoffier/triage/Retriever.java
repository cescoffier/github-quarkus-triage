package me.escoffier.triage;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.smallrye.mutiny.unchecked.Unchecked.supplier;

@ApplicationScoped
public class Retriever {

    public static final Logger LOGGER = Logger.getLogger("Retriever");

    public Uni<Project> retrieve(TriageConfig.Repository repository, GitHub github) {
        return Uni.createFrom().item(supplier(() -> {
            GHRepository project = github.getRepository(repository.repository());
            LOGGER.infof("⏳  Retrieving issues and pull requests from %s", repository.repository());
            List<GHPullRequest> prs = project.queryPullRequests().state(GHIssueState.OPEN).list().toList();
            LOGGER.infof("⚙️  Retrieved repository '%s', %d issues opened and %d pull requests opened",
                    project.getName(), project.getOpenIssueCount(),
                    prs.size());

            LOGGER.infof("⏳  Retrieving issues from %s", repository.repository());
            List<GHIssue> ghIssues = project.getIssues(GHIssueState.OPEN);
            LOGGER.infof("⏳  Retrieving pull requests from %s", repository.repository());
            List<GHPullRequest> ghPrs = project.getPullRequests(GHIssueState.OPEN);
            Map<String, List<Issue>> issues = new LinkedHashMap<>();

            List<String> listOfLabels = repository.labels().orElse(Collections.emptyList());
            if (listOfLabels.isEmpty()) {
                // Select all.
                List<Issue> list = getIssuesForTag(repository, ghIssues, ghPrs, null);
                if (!list.isEmpty()) {
                    issues.put("all", list);
                }
            } else {
                listOfLabels.forEach(s -> {
                    List<Issue> list = getIssuesForTag(repository, ghIssues, ghPrs, s);
                    if (!list.isEmpty()) {
                        issues.put(s, list);
                    }
                });
            }
            return new Project(repository.repository(), issues);
        }))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    public List<Issue> getIssuesForTag(TriageConfig.Repository repository, List<GHIssue> issues,
            List<GHPullRequest> prs, String label) {
        if (label != null) {
            LOGGER.infof("⚙️  Looking at issues with the '%s' label", label);
        }
        List<Issue> list = new ArrayList<>();
        for (GHIssue issue : issues) {
            if (hasLabel(issue, label)) {
                if (!isDropped(repository, issue)) {
                    GHPullRequest pr = getPr(issue, prs);
                    list.add(new Issue(repository, issue, pr));
                }
            }
        }
        return list;
    }

    private boolean isDropped(TriageConfig.Repository repository, GHIssue issue) {
        List<Pattern> patterns = repository.excludeLabels().orElse(Collections.emptyList());
        if (patterns.isEmpty()) {
            return false;
        }

        List<String> labels = issue.getLabels().stream().map(GHLabel::getName).collect(Collectors.toList());
        for (Pattern p : patterns) {
            for (String l : labels) {
                if (p.matcher(l).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private GHPullRequest getPr(GHIssue issue, List<GHPullRequest> prs) {
        if (issue.isPullRequest()) {
            for (GHPullRequest pr : prs) {
                if (pr.getNumber() == issue.getNumber()) {
                    return pr;
                }
            }
        }
        return null;
    }

    private boolean hasLabel(GHIssue issue, String label) {
        if (label == null) {
            return true;
        }
        return issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(s -> s.equalsIgnoreCase(label));
    }

}
