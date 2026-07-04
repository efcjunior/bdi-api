# BDI API Implementation Plan

## Project definition

- Repository and application name: `bdi-api`
- Maven coordinates: `com.coding4world:bdi-api`
- Kotlin root package: `com.coding4world.bdi.api`
- Language: English for source code, comments, API messages, documentation, and logs
- Runtime: JDK 21
- Framework: Spring Boot 4.1 with Kotlin and Spring MVC
- Database: MongoDB
- Client library: `com.coding4world:coding4world-bdi-client:1.0.0`
- API versioning: URI-based versioning under `/api/v1`

## Architecture

The application will use package-by-feature organization. Each feature will
separate HTTP adapters, application use cases, domain rules, and infrastructure
adapters without exposing persistence models through the REST API.

Primary features:

- `auth`: login, token refresh, logout, and JWT handling
- `user`: administrator-managed user accounts and roles
- `bdi`: current BDI, history, synchronization, and refresh jobs
- `shared`: configuration, error handling, observability, and rate limiting

The blocking `Coding4WorldBdiClient.current()` operation will be isolated behind
a `CurrentBdiProvider` interface. Spring MVC and imperative Spring Data MongoDB
will be used to keep a consistent blocking execution model.

## REST API

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with email and password |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate a refresh token and issue a new access token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke a refresh token |
| `GET` | `/api/v1/bdi/current` | `USER`, `ADMIN` | Return the latest known BDI and freshness status |
| `GET` | `/api/v1/bdi/history` | `USER`, `ADMIN` | Return paginated BDI history |
| `POST` | `/api/v1/admin/bdi/refresh` | `ADMIN` | Start an asynchronous refresh job |
| `GET` | `/api/v1/admin/bdi/refresh/{jobId}` | `ADMIN` | Read refresh job status and result |
| `POST` | `/api/v1/admin/users` | `ADMIN` | Create an API user |
| `PATCH` | `/api/v1/admin/users/{id}` | `ADMIN` | Change roles or enable/disable a user |

The administrative refresh endpoint will return `202 Accepted` with a `jobId`.
Job states will be `PENDING`, `RUNNING`, `SUCCEEDED`, and `FAILED`. Only one
refresh may run at a time; another request will return `409 Conflict`. Scheduled
refreshes will use the same job mechanism.

The current BDI response will contain:

```json
{
  "value": 35.08,
  "unit": "PERCENT",
  "validFrom": "2026-01-15",
  "sourcePdf": "https://example.com/source.pdf",
  "lastVerifiedAt": "2026-07-03T14:00:00Z",
  "status": "CURRENT"
}
```

If no snapshot exists, the endpoint will return `503 Service Unavailable`. A
snapshot that has not been successfully verified within 12 hours will remain
available with `status: STALE` instead of being presented as current.

## Persistence and synchronization

MongoDB will store the following collections:

- `users`: normalized email, password hash, roles, enabled state, and audit data
- `refresh_tokens`: token hash, token family, expiration, and revocation data
- `bdi_snapshots`: Decimal128 value, validity date, source, fingerprint, and timestamps
- `bdi_refresh_jobs`: refresh state, trigger, timestamps, result, and sanitized error details

Unique indexes will protect user emails, refresh token hashes, and BDI
fingerprints. A TTL index will remove expired refresh tokens. Refresh jobs will
also have a retention policy so operational history does not grow indefinitely.

Synchronization will run asynchronously at application startup and every six
hours. An unchanged publication will update `lastVerifiedAt`; a changed
fingerprint will create a new snapshot. A source failure will never delete the
last successful snapshot.

## Authentication and authorization

- Spring Security will validate RS256 JWT access tokens.
- Access tokens will expire after 15 minutes.
- Opaque 256-bit refresh tokens will expire after 7 days.
- Only SHA-256 hashes of refresh tokens will be stored.
- Refresh tokens will be single-use and rotated after every successful refresh.
- Reuse of a revoked token will revoke its entire token family.
- Passwords will use Spring Security's delegating password encoder with BCrypt.
- There will be no public registration endpoint.
- The first administrator will be bootstrapped from deployment secrets only when
  no administrator exists.
- JWT claims will include `sub`, `jti`, `iss`, `aud`, `roles`, `iat`, and `exp`.
- CORS will be disabled unless explicit trusted origins are configured.

## Rate limiting

The first release targets one application instance and will use in-memory
Bucket4j token buckets.

| Operation | Limit | Key |
| --- | --- | --- |
| Login | 5 requests/minute | Client IP |
| Token refresh | 10 requests/minute | Client IP |
| Current BDI | 60 requests/minute | Authenticated user |
| BDI history | 30 requests/minute | Authenticated user |
| Administrative operations | 5 requests/hour | Authenticated administrator |

Rejected requests will return `429 Too Many Requests`, `Retry-After`, and rate
limit headers. Forwarded client IP headers will only be trusted when a proxy is
explicitly configured. A shared rate-limit store will be required before the
application is scaled to multiple instances.

## Error handling and observability

- Errors will use RFC 9457 Problem Details in English.
- Error responses will include a stable application error code and trace ID.
- Logs will be structured and must not contain passwords, JWTs, refresh tokens,
  or private key material.
- Spring Boot Actuator will expose a sanitized health endpoint.
- OpenAPI documentation and Swagger UI will be public only in the local profile
  and restricted to administrators in production.

## Test strategy

- Unit tests for freshness calculation, fingerprint deduplication, token
  lifecycle, authorization rules, mappings, and rate limits
- MockMvc tests for validation, authentication, versioned endpoints, pagination,
  and Problem Details responses
- MongoDB integration tests using Testcontainers
- Integration tests for indexes, token rotation, refresh job transitions, and
  BDI snapshot persistence
- A fake `CurrentBdiProvider` for deterministic automated tests
- A separately tagged `live` test for the real external source, excluded from
  the standard CI pipeline
- Minimum target of 80% line coverage
- `ktlint`, `detekt`, and the complete test suite executed by `mvn verify`

## Delivery schedule

The estimate assumes one developer and covers July 6 through July 17, 2026.

Implementation progress:

- [x] Contract and bootstrap
- [x] Domain and MongoDB
- [x] BDI integration
- [ ] Asynchronous synchronization
- [ ] Security
- [ ] REST API
- [ ] Rate limiting
- [ ] Delivery tooling
- [ ] Hardening and release

| Date | Stage | Deliverables | Completion gate |
| --- | --- | --- | --- |
| July 6 | Contract and bootstrap | Maven project, wrapper, package structure, profiles, dependency repository, and configuration properties | Application starts with the local profile |
| July 7 | Domain and MongoDB | Domain models, repositories, explicit indexes, auditing, and Testcontainers setup | Persistence integration tests pass |
| July 8 | BDI integration | Client adapter, snapshots, fingerprinting, deduplication, and freshness calculation | Current and stale scenarios pass |
| July 9 | Asynchronous synchronization | Refresh job model, executor, startup refresh, six-hour scheduler, concurrency control, and job status endpoint | Refresh lifecycle tests pass |
| July 10 and 13 | Security | Users, administrator bootstrap, login, RS256 JWT, refresh rotation, logout, and role authorization | Authentication and authorization tests pass |
| July 14 | REST API | Versioned controllers, validation, pagination, RFC 9457 errors, and OpenAPI documentation | API contract tests pass |
| July 15 | Rate limiting | Bucket4j policies, identity keys, response headers, and rejection behavior | Rate-limit tests pass |
| July 16 | Delivery tooling | Docker Compose, Actuator, structured logging, README, and GitHub Actions | CI builds and verifies the application |
| July 17 | Hardening and release | Security review, failure testing, acceptance checks, and `v1.0.0` preparation | `mvn verify` passes and release checklist is complete |

## Acceptance criteria

- Every project-owned text is written in English.
- Protected endpoints reject missing, invalid, expired, or unauthorized tokens.
- Refresh token rotation and reuse detection work as specified.
- The API distinguishes `CURRENT` and `STALE` data using the 12-hour threshold.
- The scheduler and administrator endpoint cannot run overlapping refreshes.
- A failed external lookup preserves the latest successful BDI snapshot.
- BDI history is paginated and does not contain duplicate publications.
- Rate-limited requests return the documented status and headers.
- MongoDB integration tests and the complete Maven verification pipeline pass.
- Local startup is documented and reproducible with Docker Compose.

## Assumptions

- The first release runs as a single application instance.
- Users are created by administrators; public registration is out of scope.
- BDI history is available only to authenticated users.
- The source is checked every six hours and considered stale after 12 hours
  without a successful verification.
- Kubernetes deployment and distributed rate limiting are deferred until
  horizontal scaling is required.
