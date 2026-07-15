# BDI API

`bdi-api` is a versioned REST API for current and historical BDI information.
It is built with Kotlin, JDK 21, Spring Boot, and MongoDB.

## Requirements

- JDK 21
- Docker, or a locally available MongoDB instance
- GitHub Packages credentials with permission to read `efcjunior/bdi-client`
- A running `auth-api` instance for JWT issuing and JWKS validation

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
keeping a local `.env` makes changes explicit. The `local` profile expects an
`auth-api` JWKS endpoint; by default it uses `http://localhost:8080/api/v1/auth/jwks`.

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
`mongo` service, validates JWTs against `AUTH_JWKS_URI`, exposes port `8080`,
and emits structured console logs in Logstash JSON format.

The versioned API is exposed under `/api/v1`. With the `local` profile,
Swagger UI is available at `http://localhost:8080/swagger-ui.html` and the
OpenAPI document at `http://localhost:8080/v3/api-docs`. Outside the local
profile, both documentation endpoints require an administrator access token.

Main endpoints:

- `GET /api/v1/bdi/current`
- `GET /api/v1/bdi/history?page=0&size=20`
- `POST /api/v1/admin/bdi/refresh`
- `GET /api/v1/admin/bdi/refresh/{jobId}`

Authentication, refresh tokens, users, passwords, and private signing keys are owned by `auth-api`.

## Security configuration

`bdi-api` is a Resource Server. Configure it with the issuer, audience, and JWKS endpoint exposed by `auth-api`:

```bash
AUTH_JWT_ISSUER=http://auth-api:8080
AUTH_JWT_AUDIENCE=bdi-api
AUTH_JWKS_URI=http://auth-api:8080/api/v1/auth/jwks
```

Access tokens must contain:

- issuer matching `AUTH_JWT_ISSUER`;
- audience containing `AUTH_JWT_AUDIENCE`;
- a `roles` claim with values such as `USER` or `ADMIN`.

## Rate limiting

The first release uses in-memory Bucket4j buckets and is intended for a single
application instance. Requests over the limit return `429 Too Many Requests`
with RFC 9457 Problem Details, `Retry-After`, `RateLimit-*`, and
`X-RateLimit-*` headers.

Default policies:

| Operation | Limit | Key |
| --- | --- | --- |
| Current BDI | 60 requests/minute | Authenticated user |
| BDI history | 30 requests/minute | Authenticated user |
| Administrative BDI operations | 5 requests/hour | Authenticated administrator |

Login, refresh, and user administration rate limits are owned by `auth-api`.
A shared rate-limit store is required before running multiple application
instances.

## Delivery tooling

The repository includes:

- `Dockerfile`: runtime image for an already-built Spring Boot JAR.
- `docker-compose.yml`: local MongoDB and API services with health checks.
- `.env.example`: safe local environment template.
- `.github/workflows/ci.yml`: GitHub Actions workflow that runs Maven verify on JDK 21.

The Docker image intentionally does not run Maven inside the image build. Build
the JAR before `docker compose up --build`; this avoids passing GitHub Packages
credentials into Docker build layers.

CI uses GitHub Packages authentication through `actions/setup-java` with server
ID `github`. If the package `efcjunior/bdi-client` is not readable by the
automatic repository token, configure repository secret `GH_PACKAGES_TOKEN`
with a token that has `read:packages` permission.

Operational endpoints:

- `GET /actuator/health`
- `GET /actuator/info`

Health details are sanitized by default.

## Hardening and release

The build is hardened with Maven Enforcer rules:

- JDK 21 is required.
- Maven 3.9 or newer is required.

The Spring Boot build also generates build metadata for Actuator info.

Before publishing `v1.0.0`, review:

- [CHANGELOG.md](CHANGELOG.md)
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md)
- [SECURITY.md](SECURITY.md)

Recommended release validation:

```shell
./mvnw -Dkotlin.compiler.daemon=false clean verify
docker compose config
docker compose build api
```

## Verify

```shell
./mvnw -Dkotlin.compiler.daemon=false clean verify
```

## Package organization

Code is organized by feature under `com.coding4world.bdi.api`:

- `auth`: resource-server JWT validation and authorization
- `bdi`: BDI snapshots and refresh jobs
- `shared`: cross-cutting configuration and infrastructure

Each feature keeps its HTTP adapters, application use cases, domain rules, and
infrastructure adapters separate.
