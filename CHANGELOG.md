# Changelog

All notable changes to this project will be documented in this file.

## 1.0.0 - Unreleased

### Added

- Versioned REST API under `/api/v1`.
- JWT authentication with RS256 access tokens.
- Opaque refresh token rotation and reuse detection.
- Administrator bootstrap from deployment secrets.
- Administrator-managed users and roles.
- Current BDI endpoint with `CURRENT` and `STALE` freshness status.
- Paginated BDI history endpoint.
- Asynchronous BDI refresh jobs with scheduler and administrator trigger.
- MongoDB persistence with indexes for users, refresh tokens, BDI snapshots, and
  refresh jobs.
- RFC 9457 Problem Details responses with stable application error codes and
  trace IDs.
- OpenAPI and Swagger UI configuration.
- In-memory Bucket4j rate limiting for public, user, and administrator
  operations.
- Docker Compose local runtime with MongoDB.
- GitHub Actions CI for Maven verification and Docker build validation.

### Security

- Production startup requires configured RSA key material.
- Passwords use Spring Security's delegating password encoder.
- Refresh tokens are stored as SHA-256 hashes only.
- Health details are sanitized by default.
- Forwarded IP headers are ignored unless explicitly trusted.
