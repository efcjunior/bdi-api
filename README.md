# BDI API

`bdi-api` is a versioned REST API for current and historical BDI information.
It is built with Kotlin, JDK 21, Spring Boot, and MongoDB.

## Requirements

- JDK 21
- Docker, or a locally available MongoDB instance
- GitHub Packages credentials with permission to read `efcjunior/bdi-client`

## GitHub Packages authentication

Add the following server to `~/.m2/settings.xml`. The server ID must remain
`github` because it matches the repository declared in `pom.xml`.

```xml
<server>
    <id>github</id>
    <username>${env.GITHUB_USERNAME}</username>
    <password>${env.GITHUB_TOKEN}</password>
</server>
```

Set `GITHUB_USERNAME` and a GitHub personal access token with the
`read:packages` permission before building the application.

## Run locally

Start MongoDB, then run:

```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application health endpoint is available at
`http://localhost:8080/actuator/health`.

## Verify

```shell
./mvnw clean verify
```

## Package organization

Code is organized by feature under `com.coding4world.bdi.api`:

- `auth`: authentication and token lifecycle
- `user`: user accounts and roles
- `bdi`: BDI snapshots and refresh jobs
- `shared`: cross-cutting configuration and infrastructure

Each feature will keep its HTTP adapters, application use cases, domain rules,
and infrastructure adapters separate.
