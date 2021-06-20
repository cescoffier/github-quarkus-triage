package me.escoffier.triage;

import io.smallrye.mutiny.Uni;
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

    @Inject
    TriageConfig repositories;

    @CommandLine.Option(names = "-o", description = "Output file", defaultValue = "triage.html")
    String output;

    @Inject
    public TriageCommand(GitHub github) {
        this.github = github;
    }

    @Inject Retriever retriever;

    @Override
    public void run() {
        LOGGER.infof("⚙️  Reading %d repositories...", repositories.repositories().size());

        GHMyself myself;
        try {
            myself = github.getMyself();
            LOGGER.infof("⚙️  Connected as %s", myself.getName());
        } catch (IOException e) {
            LOGGER.error("Unable to retrieve the issues from Github", e);
            return;
        }

        List<Uni<Project>> unis = repositories.repositories().stream()
                .map(r -> retriever.retrieve(r, github)).collect(Collectors.toList());
        List<Project> projects = Uni.combine().all().unis(unis)
                .combinedWith(list -> (List<Project>) list)
                .await().indefinitely();

        try {
            generateReport(projects);
        } catch (IOException e) {
            LOGGER.error("Unable to write the triage report", e);
        }
    }

    private void generateReport(List<Project> projects) throws IOException {
        long count = projects.stream().flatMap(p -> p.issues.values().stream()).mapToLong(Collection::size).sum();
        final String content;

        if (output.endsWith(".md")) {
            content = Templates.triageReport(projects, count, new SimpleDateFormat("dd-MM-yyyy").format(new Date()))
                    .render();
        } else {
            content = Templates.report(projects, count, new SimpleDateFormat("dd-MM-yyyy").format(new Date()))
                    .render();
        }

        File out = new File(output);
        Files.write(out.toPath(), content.getBytes(StandardCharsets.UTF_8));
        LOGGER.infof("\uD83C\uDF7B  Report generated: %s", out.getAbsolutePath());
    }


}
