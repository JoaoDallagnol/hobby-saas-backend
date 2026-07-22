---
name: run-api-acceptance
description: Execute, diagnose, maintain and report the Hobby SaaS API acceptance suite built with Postman Collection v2.1, Postman CLI, Docker Compose and PostgreSQL. Use when asked to run API end-to-end tests, validate the Postman collection, investigate an acceptance failure or logs, update API acceptance coverage after a contract change, or prepare the isolated Postman environment for manual testing.
---

# Run API acceptance

Run the repository-owned acceptance workflow without touching the regular local database. Treat the collection as client-level acceptance coverage; keep JUnit/Testcontainers as the source for exhaustive domain and persistence checks.

## Required context

- Read `AGENTS.md` and `docs/api-acceptance-testing.md` completely.
- Read `docs/roadmap.md` before changing coverage or contracts.
- If a failure concerns a feature, read its routed documentation from `AGENTS.md` before changing behavior.
- Do not load real provider credentials. The default suite must remain local and deterministic.

## Execute

1. Run `./scripts/run-api-acceptance.sh` from the repository root.
2. On failure, preserve evidence: read `target/postman/postman-results.json`, then inspect `docker compose -f postman/docker-compose.acceptance.yml ps` and logs for `app`, `postgres` and `s3mock`.
3. Classify the failure as application, contract, fixture, infrastructure or collection-test failure before editing.
4. Verify persisted state with read-only SQL when the HTTP response is insufficient. Never print tokens or sensitive environment values.
5. Fix the narrowest correct layer, rerun the focused folder if useful, then rerun the complete script.
6. Run `mvn test`, `mvn clean install`, `git diff --check` and `./scripts/check-no-secrets.sh` after application or contract changes.
7. Report request/assertion counts, failures, health, migrations, relevant log evidence and untested external integrations.

Use `./scripts/run-api-acceptance.sh --keep` to retain the isolated stack after an automated run. Use `./scripts/run-api-acceptance.sh --prepare-only` to prepare users and Plus entitlement for manual execution in Postman.

## Maintain

- Keep `postman/HobbySaaS.postman_collection.json` in Collection v2.1 while JSON import and the built-in JSON/JUnit reporters are required.
- Keep importable defaults in `postman/local.postman_environment.json`; never commit real bearer tokens or provider credentials.
- Give every mutation a response assertion and every created resource a collection variable for dependent requests.
- Include negative authentication, authorization/IDOR, validation, Free/Plus and disabled-feature scenarios.
- Use unique run data and a disposable Compose project. Never point reset/fixture commands at the normal local or production database.
- Do not create a public upgrade/test endpoint. Provision Plus only through the isolated runner fixture.
- When an endpoint changes, update OpenAPI/controller tests, the collection, this workflow documentation and affected product documents in the same work.
- Do not claim Firebase, R2, Places or production behavior as validated by the default local suite; use the external-integration checklist in the tutorial.

## Manual Postman

Prepare with `--prepare-only`, import both JSON files under `postman/`, select the “Hobby SaaS Local Acceptance” environment and run the collection in order. Stop and remove the isolated environment afterward with:

```bash
docker compose -f postman/docker-compose.acceptance.yml down -v --remove-orphans
```
