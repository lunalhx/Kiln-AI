# Kiln-AI

Kiln-AI is an AI learning system designed to help users develop durable, transferable, and independently usable capability. Its core measure is what a user can do after AI assistance has faded.

## Current Slice

This repository starts with a runnable Learning vertical slice:

- Create a `Concept`, the smallest unit that can be taught and assessed independently.
- Record an assessed `LearningEvent`.
- Update `LearnerConceptProgress` with deterministic domain rules.
- Schedule a first T+1 day review after a learner first reaches `INDEPENDENT`.

AI, RAG, ingestion, and authentication are intentionally out of scope for this initial skeleton. They will be infrastructure or adjacent bounded contexts, not owners of learning-state rules.

## Six-Module Hexagonal Architecture

The repository is a Maven multi-module project. Dependencies point inward:

```text
kiln-ai-app
    -> kiln-ai-trigger -> kiln-ai-api
    -> kiln-ai-infrastructure -> kiln-ai-domain -> kiln-ai-types

kiln-ai-trigger -----------------> kiln-ai-domain / kiln-ai-types
```

| Module | Responsibility | Allowed dependencies |
| --- | --- |
| `kiln-ai-types` | Cross-module error codes and shared types | None |
| `kiln-ai-domain` | Aggregates, value objects, domain services, and output ports | `kiln-ai-types` |
| `kiln-ai-infrastructure` | PostgreSQL, Flyway, MyBatis, and output-port adapters | `kiln-ai-domain` |
| `kiln-ai-api` | HTTP request / response contracts, independent from domain implementation | Validation API only |
| `kiln-ai-trigger` | HTTP controllers, exception mapping, jobs and listeners | `api`, `domain`, `types` |
| `kiln-ai-app` | Spring Boot composition root, runtime configuration, executable JAR | `trigger`, `infrastructure` |

The domain owns output ports under `domain/**/adapter/port`. Infrastructure implements those ports under `infrastructure/adapter/repository`. HTTP contracts live in `api`, while Controllers are inbound adapters under `trigger/http`. The app module only assembles the runtime. The domain does not depend on Spring, MyBatis, PostgreSQL, or future AI SDKs.

The learning bounded context currently contains:

- `Concept` in `domain.content`.
- `LearnerConceptProgress` and `LearningEvent` in `domain.learning`.
- `LearningWorkflow` in `domain.pedagogy`.
- `ReviewTask` in `domain.review`.

An ArchUnit test in `kiln-ai-domain` prevents the domain from depending on Spring, MyBatis, or JPA.

## Prerequisites

- Java 21
- Docker Desktop

## Run Locally

1. Create local environment settings:

   ```bash
   cp deploy/local/.env.example deploy/local/.env
   ```

   Fill `OPENAI_API_KEY` in `deploy/local/.env` with the OpenCode Go key. Startup and Docker Compose both read that file. Tests do not.

2. Start PostgreSQL:

   ```bash
   docker compose --env-file deploy/local/.env -f deploy/local/compose.yaml up -d
   ```

3. Run the application module:

   ```bash
   ./mvnw -pl kiln-ai-app -am spring-boot:run
   ```

Flyway applies the initial schema automatically. Confirm readiness at `http://localhost:8080/actuator/health`.

## Try The Learning Slice

Create a concept:

```bash
curl -X POST http://localhost:8080/api/concepts \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Sunk cost",
    "summary": "Irrecoverable past cost that should not determine a current marginal decision.",
    "sourceReference": "Economics chapter 3"
  }'
```

Use the returned `id` as `conceptId`, then record a no-hint independent success:

```bash
curl -X POST http://localhost:8080/api/concepts/<conceptId>/learning-events \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "<userId>",
    "eventType": "INDEPENDENT_TEST",
    "result": "PASS",
    "hintLevel": 0,
    "delayedReview": false,
    "transfer": false,
    "occurredAt": "2026-08-13T00:00:00Z"
  }'
```

The response reports the current `state`, deterministic `nextAction`, and the first review due time where applicable.

## Domain Rules Already Enforced

- A passed task with `hintLevel` above zero is `ASSISTED`, not `INDEPENDENT` evidence.
- Explanations and hints cannot become independent evidence.
- `DURABLE` requires both a no-hint delayed success and a no-hint transfer success.
- A failure after `INDEPENDENT` or `DURABLE` downgrades the state to `UNDERSTOOD` and invalidates prior independent evidence.

## Verify

```bash
./mvnw clean test
```
