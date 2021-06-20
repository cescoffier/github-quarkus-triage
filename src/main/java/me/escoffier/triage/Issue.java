package me.escoffier.triage;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Issue {

    public final String title;
    public final String url;
    public final int number;

    public final boolean updated;
    public final boolean stale;

    public final boolean isPR;
    public final boolean isDraft;

    public final List<String> labels;
    public final String assignee;

    public Issue(TriageConfig.Repository repository, GHIssue issue, GHPullRequest pr) {
        this.title = issue.getTitle();
        this.url = issue.getHtmlUrl().toExternalForm();
        this.number = issue.getNumber();

        Optional<Date> updateAt = get("updateAt", this.url, issue::getUpdatedAt);
        if (updateAt.isPresent()) {
            this.updated = updateAt.get().toInstant().isAfter(new Date().toInstant().minus(7, ChronoUnit.DAYS));
            this.stale = updateAt.get().toInstant().isBefore(new Date().toInstant().minus(6 * 30, ChronoUnit.DAYS));
        } else {
            this.updated = false;
            this.stale = false;
        }

        Optional<String> assignee = get("assignee", this.url, issue::getAssignee)
                .flatMap(gh -> get("name", this.url, gh::getName));
        this.assignee = assignee.orElse(null);

        this.labels = issue.getLabels().stream()
                .map(GHLabel::getName)
                .filter(s -> ! isHidden(repository, s))
                .collect(Collectors.toList());

        isPR = issue.isPullRequest();
        if (pr != null) {
            isDraft = get("draft", this.url, pr::isDraft).orElse(false);
        } else {
            isDraft = false;
        }
    }

    private boolean isHidden(TriageConfig.Repository repository, String s) {
        List<Pattern> list = repository.hideLabels().orElse(Collections.emptyList());
        for (Pattern p : list) {
            if (p.matcher(s).matches())
                return true;
        }
        return false;
    }

    private <T> Optional<T> get(String attribute, String issue, ThrowingSupplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Exception e) {
            Logger.getLogger("Triage").warnf("Unable to retrieve %s from issue ", attribute, issue, e);
            return Optional.empty();
        }
    }

    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

}
