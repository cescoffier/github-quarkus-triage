package me.escoffier.triage;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;
import picocli.CommandLine;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command
public class TriageCommand implements Runnable {

    private final GitHub github;

    private static final Logger LOGGER = Logger.getLogger("Triage");

    @ConfigProperty(name = "github.labels") List<String> labels;
    @ConfigProperty(name = "github.repository") String repository;

    @CommandLine.Option(names = "-o", description = "Output HTML file", defaultValue = "triage.html")
    String output;

    @Inject
    public TriageCommand(GitHub github) {
        this.github = github;
    }

    @Override
    public void run() {
        LOGGER.info("⚙️  Initializing...");

        try {
            GHMyself myself = github.getMyself();
            LOGGER.infof("⚙️  Connected as %s", myself.getName());

            GHRepository repository = github.getRepository(this.repository);
            List<GHPullRequest> prs = repository.queryPullRequests().state(GHIssueState.OPEN).list().toList();
            LOGGER.infof("⚙️  Retrieved repository '%s', %d issues opened and %d pull requests opened",
                    repository.getName(), repository.getOpenIssueCount(),
                    prs.size());

            LOGGER.infof("⏳  Retrieving issues...");
            List<GHIssue> ghIssues = repository.getIssues(GHIssueState.OPEN);
            Map<String, List<Issue>> issues = new LinkedHashMap<>();
            labels.forEach(s -> {
                List<Issue> list = getIssuesForTag(ghIssues, s);
                if (! list.isEmpty()) {
                    issues.put(s, list);
                }
            });

            generateReport(issues);

        } catch (IOException e) {
            LOGGER.error("Unable to retrieve the issues from Github", e);
        }
    }

    private void generateReport(Map<String, List<Issue>> issues) throws IOException {
        long count = issues.values().stream().mapToLong(Collection::size).sum();
        String content = Templates.report(issues, count, new SimpleDateFormat("dd-MM-yyyy").format(new Date()))
                .render();
        File out = new File(output);
        Files.write(out.toPath(), content.getBytes(StandardCharsets.UTF_8));
        LOGGER.infof("\uD83C\uDF7B  Report generated: %s", out.getAbsolutePath());
    }

    public List<Issue> getIssuesForTag(List<GHIssue> issues, String label) {
        LOGGER.infof("⚙️  Looking at issues with the '%s' label", label);
        return issues.stream()
                .filter(i -> hasLabel(i, label))
                .map(Issue::new)
                .collect(Collectors.toList());
    }

    private boolean hasLabel(GHIssue issue, String label) {
        return issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(s -> s.equalsIgnoreCase(label));
    }
}
