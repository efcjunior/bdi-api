# BDI API Implementation Plan

## Current state

`bdi-api` is now a BDI Resource Server. It owns BDI snapshots, BDI history, BDI refresh jobs, scheduling, BDI-specific rate limits, and BDI persistence only.

Authentication issuing, refresh tokens, user management, password storage, and private signing keys were extracted to `auth-api`.

## REST API

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/bdi/current` | `USER`, `ADMIN` | Return the latest known BDI and freshness status |
| `GET` | `/api/v1/bdi/history` | `USER`, `ADMIN` | Return paginated BDI history |
| `POST` | `/api/v1/admin/bdi/refresh` | `ADMIN` | Start an asynchronous refresh job |
| `GET` | `/api/v1/admin/bdi/refresh/{jobId}` | `ADMIN` | Read refresh job status and result |

Removed endpoints now belong to `auth-api`:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/admin/users`
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/users/{id}`
- `PATCH /api/v1/admin/users/{id}`

## Security and persistence

Configure JWT validation with:

```yaml
bdi-api:
  security:
    jwt:
      issuer: ${AUTH_JWT_ISSUER}
      audience: ${AUTH_JWT_AUDIENCE:bdi-api}
      jwks-uri: ${AUTH_JWKS_URI}
```

MongoDB collections owned by `bdi-api`:

- `bdi_snapshots`
- `bdi_refresh_jobs`

Collections owned by `auth-api`:

- `users`
- `refresh_tokens`

## Implementation progress

- [x] Contract and bootstrap
- [x] Domain and MongoDB
- [x] BDI integration
- [x] Asynchronous synchronization
- [x] Original local security implementation
- [x] REST API
- [x] Rate limiting
- [x] Delivery tooling
- [x] Hardening and release
- [x] Resource-server migration after auth extraction

## Test strategy

- Missing token returns `401`.
- Token with wrong issuer returns `401`.
- Token with wrong audience returns `401`.
- Token with `USER` role can access BDI current/history.
- Token with `USER` role cannot access BDI admin refresh.
- Token with `ADMIN` role can access BDI admin refresh.
- Removed auth/user endpoints are absent from `bdi-api`.
- MongoDB integration tests cover only BDI-owned collections and indexes.
- Rate-limit tests cover only BDI and BDI administration policies.
- `./mvnw -Dkotlin.compiler.daemon=false clean verify` passes.
