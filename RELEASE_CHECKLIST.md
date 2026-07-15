# Release checklist

Use this checklist before publishing `v1.0.0`.

## Source and dependency checks

- [ ] `main` is up to date and contains only reviewed changes.
- [ ] The GitHub Actions `CI` workflow passes on the release commit.
- [ ] `./mvnw -Dkotlin.compiler.daemon=false clean verify` passes locally.
- [ ] `docker compose config` succeeds.
- [ ] `docker compose build api` succeeds after the JAR is built.
- [ ] GitHub repository secret `GH_PACKAGES_TOKEN` exists and can read `efcjunior/bdi-client`.

## Security checks

- [ ] No passwords, JWTs, refresh tokens, private signing keys, or PATs are committed.
- [ ] Any token accidentally shared during development has been revoked.
- [ ] Production `AUTH_JWT_ISSUER`, `AUTH_JWT_AUDIENCE`, and `AUTH_JWKS_URI` point to the deployed `auth-api`.
- [ ] CORS remains disabled unless explicit trusted origins are configured.
- [ ] Forwarded IP headers are trusted only behind a controlled reverse proxy.
- [ ] Actuator health details remain sanitized in production.

## Acceptance checks

- [ ] Protected endpoints reject missing, invalid, expired, or unauthorized tokens.
- [ ] `bdi-api` rejects tokens with the wrong issuer or audience.
- [ ] Removed auth/user endpoints are not available in `bdi-api`.
- [ ] Failed external BDI synchronization preserves the latest successful snapshot.
- [ ] Current BDI distinguishes `CURRENT` and `STALE`.
- [ ] BDI history is paginated.
- [ ] Rate-limited requests return `429`, `Retry-After`, and rate-limit headers.
- [ ] Local startup is reproducible with Docker Compose.

## Release steps

- [ ] Update this checklist with the final verification result.
- [ ] Create release tag `v1.0.0`.
- [ ] Publish GitHub Release notes from `CHANGELOG.md`.
- [ ] Decide whether to publish a Docker image or Maven artifact for `bdi-api`.
