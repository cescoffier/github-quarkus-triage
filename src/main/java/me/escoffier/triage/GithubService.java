package me.escoffier.triage;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.io.IOException;

public class GithubService {

    @ApplicationScoped
    @Produces
    public GitHub init(@ConfigProperty(name = "github.token") String token) throws IOException {
        return new GitHubBuilder().withOAuthToken(token).build();
    }

}
