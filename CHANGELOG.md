# Changelog

## Unreleased

- Bootstrap `bdi-api` Spring Boot project.
- BDI current and history endpoints.
- Resource-server JWT validation using external `auth-api` issuer, audience, and JWKS.
- Rate limiting with RFC-style headers for BDI and administration endpoints.
- BDI client integration with source configuration loaded from resources.
- Asynchronous BDI refresh jobs with scheduler and administrator trigger.
- MongoDB persistence with indexes for BDI snapshots and refresh jobs.
- Delivery tooling with Dockerfile, Docker Compose, GitHub Actions CI, release checklist, and security notes.
