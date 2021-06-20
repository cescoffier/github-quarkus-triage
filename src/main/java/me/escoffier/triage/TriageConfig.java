package me.escoffier.triage;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@ConfigMapping(prefix = "github")
public interface TriageConfig {

    String token();

    List<Repository> repositories();

    interface Repository {
        String repository();
        Optional<List<String>> labels();

        Optional<List<Pattern>> excludeLabels();

        Optional<List<Pattern>> hideLabels();
    }

}
