# Quarkus / GitHub Triage tool

_Just a personal tool to generate a report about the Quarkus issue I need to look at._

## Build

```shell
> mvn package
```

## Run

```shell
# Make sure you have a GITHUB_TOKEN env variable that give you access to the Github API
> java -jar target/quarkus-app/quarkus-run.jar
> open triage.html
```

## Configuration and Options

* the `-o` option allows configuring the output file, default is `triage.html`
* the GitHub token is read from the `github.token` configuration property. It can be configured in the `application.yaml` file, as system variable (`-Dgithub.token=...`) or env variable (`GITHUB_TOKEN=...`)
* the repository and areas of responsibilities are configured in the `application.yaml` file:

```yaml
github:
  token: .... # mandatory, but can be configured from an env variable
  repositories:
    - repository: quarkusio/quarkus # name of the repository  
      labels:   # list of labels to include in the report
        - area/adr
        - area/grpc
        - area/kafka
        - area/mailer
        - area/mutiny
        - area/mongo
        - area/reactive-messaging
        - area/reactive-sql-clients
        - area/reactive-streams-operators
        - area/reactive
        - area/redis
        - area/vertx
      exclude-labels: # optional list of labels to not include in the report, even if they match a label from above
        - kind/extension-proposal # Can be a regexp (Java pattern)
      hide-labels: # optional list of labels to hide in the report, the issue lis listed, but the label is not displayed
        - pinned
        - "area/.*" # can be regexp (Java pattern)
```

A full example of config is the following:

```yaml
github:
  repositories:
    - repository: quarkusio/quarkus
      labels:
        - area/adr
        - area/grpc
        - area/kafka
        - area/mailer
        - area/mutiny
        - area/mongo
        - area/reactive-messaging
        - area/reactive-sql-clients
        - area/reactive-streams-operators
        - area/reactive
        - area/redis
        - area/vertx
      exclude-labels:
        - kind/extension-proposal
      hide-labels:
        - pinned
        - "area/.*"

    - repository: smallrye/smallrye-reactive-messaging
      exclude-labels:
        - help wanted
      
    - repository: smallrye/smallrye-mutiny
```