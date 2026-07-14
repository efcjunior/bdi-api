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

The versioned API is exposed under `/api/v1`. With the `local` profile,
Swagger UI is available at `http://localhost:8080/swagger-ui.html` and the
OpenAPI document at `http://localhost:8080/v3/api-docs`. Outside the local
profile, both documentation endpoints require an administrator access token.

Main endpoints:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/bdi/current`
- `GET /api/v1/bdi/history?page=0&size=20`
- `POST /api/v1/admin/bdi/refresh`
- `GET /api/v1/admin/bdi/refresh/{jobId}`
- `POST /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{userId}`

## Security configuration

Production requires an RSA key pair supplied through `JWT_PUBLIC_KEY` and
`JWT_PRIVATE_KEY`. The public key must use X.509 PEM format and the private key
must use unencrypted PKCS#8 PEM format. Newlines may be supplied directly or as
escaped `\n` sequences.

The initial administrator is created only when no administrator exists. Set
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` together; the password
must contain at least 12 characters. Remove these variables after the account
has been created. The `local` and `test` profiles use ephemeral RSA keys and
must not be used in production.

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
