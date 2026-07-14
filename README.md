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

Optionally create a local environment file:

```shell
cp .env.example .env
```

Docker Compose provides safe development defaults when `.env` is absent, but
keeping a local `.env` makes changes explicit. The `local` profile uses
ephemeral RSA keys, so `JWT_PUBLIC_KEY` and `JWT_PRIVATE_KEY` are not required
for local development.

Start MongoDB with Docker Compose:

```shell
docker compose up -d mongo
```

Then run the API from the host:

```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application health endpoint is available at
`http://localhost:8080/actuator/health`.

## Run locally with Docker Compose

Build the application JAR first:

```shell
./mvnw -Dkotlin.compiler.daemon=false clean package
```

Then start MongoDB and the API:

```shell
docker compose up --build
```

The API container uses the `local` profile, reads `.env`, connects to the
`mongo` service, exposes port `8080`, and emits structured console logs in
Logstash JSON format.

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

## Rate limiting

The first release uses in-memory Bucket4j buckets and is intended for a single
application instance. Requests over the limit return `429 Too Many Requests`
with RFC 9457 Problem Details, `Retry-After`, `RateLimit-*`, and
`X-RateLimit-*` headers.

Default policies:

| Operation | Limit | Key |
| --- | --- | --- |
| Login | 5 requests/minute | Client IP |
| Token refresh | 10 requests/minute | Client IP |
| Current BDI | 60 requests/minute | Authenticated user |
| BDI history | 30 requests/minute | Authenticated user |
| Administrative operations | 5 requests/hour | Authenticated administrator |

Forwarded client IP headers are ignored by default. Enable
`bdi-api.rate-limit.trust-forwarded-headers=true` only when the application is
behind a trusted reverse proxy that controls `X-Forwarded-For` or `X-Real-IP`.
A shared rate-limit store is required before running multiple application
instances.

## Delivery tooling

The repository includes:

- `Dockerfile`: runtime image for an already-built Spring Boot JAR.
- `docker-compose.yml`: local MongoDB and API services with health checks.
- `.env.example`: safe local environment template.
- `.github/workflows/ci.yml`: GitHub Actions workflow that runs Maven verify on
  JDK 21.

The Docker image intentionally does not run Maven inside the image build. Build
the JAR before `docker compose up --build`; this avoids passing GitHub Packages
credentials into Docker build layers.

CI uses GitHub Packages authentication through `actions/setup-java` with server
ID `github`. If the package `efcjunior/bdi-client` is not readable by the
repository `GITHUB_TOKEN`, configure a repository secret with a token that has
`read:packages` permission and expose it as `GITHUB_TOKEN` or adjust the
workflow secret name.

Operational endpoints:

- `GET /actuator/health`
- `GET /actuator/info`

Health details are sanitized by default.

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
