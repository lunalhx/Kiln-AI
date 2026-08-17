# Kiln-AI

Kiln-AI is an AI learning system designed to help users develop durable, transferable, and independently usable capability. Its core measure is what a user can do after AI assistance has faded.

## Current Slice

The repository ships one end-to-end product path: the **Learning/Practice reference**. A learner is diagnosed on a bounded, no-hint task in `zh-CN`, and a demonstrated gap leads through the guarded loop — Explain teaching, fresh Apply Practice with a five-level Hint Ladder, and anchored Teach-back — before a fresh Independent Test can be reopened. A passing Diagnostic routes through a neutral transition to a fresh Independent Test; a post-remediation Independent pass produces Independent Learning Evidence and schedules the Delayed Review cadence (1, 3, 7, 21 days).

The flow is:

`Apply Diagnostic -> (neutral transition after pass) -> fresh equivalent Apply Independent Test -> task verification -> assessment and verification -> Independent Evidence`

and a failed Diagnostic enters `Explain -> Apply Practice (Hints) -> Teach-back -> fresh Independent Test` under the deterministic Workflow Guard and the bounded Pedagogy Agent.

The five first-party Skill Bundles (`apply.task-first`, `reasoning.rule-application`, `representation.formal-expression`, `verification.structured-task-contract`, `subject.calculus-notation`) are frozen, versioned, and immutable; the Explain, Hint, and Teach-back reference stacks add one Action Bundle each over the shared immutable `subject.calculus-notation@1.0.0`. Only the Action Bundle contributes draft fields. Each Profile compiles an immutable English system prompt and receives execution data as a closed JSON object.

RAG, ingestion, authentication, Learner Memory, and the other four Teaching Node Profiles (Explain, Retrieve, Teach-back, Hint) are intentionally out of scope for this slice.

## Six-Module Hexagonal Architecture

The repository is a Maven multi-module project. Dependencies point inward:

```text
kiln-ai-app
    -> kiln-ai-trigger -> kiln-ai-api
    -> kiln-ai-infrastructure -> kiln-ai-domain -> kiln-ai-types

kiln-ai-trigger -----------------> kiln-ai-domain / kiln-ai-types
```

| Module | Responsibility | Allowed dependencies |
| --- | --- | --- |
| `kiln-ai-types` | Cross-module error codes and shared types | None |
| `kiln-ai-domain` | Aggregates, value objects, domain services, and output ports | `kiln-ai-types` |
| `kiln-ai-infrastructure` | PostgreSQL, Flyway, MyBatis, operator catalog, and output-port adapters | `kiln-ai-domain` |
| `kiln-ai-api` | HTTP request / response contracts, independent from domain implementation | Validation API only |
| `kiln-ai-trigger` | HTTP controllers, exception mapping, jobs and listeners | `api`, `domain`, `types` |
| `kiln-ai-app` | Spring Boot composition root, runtime configuration, executable JAR | `trigger`, `infrastructure` |

The domain owns output ports under `domain/**/adapter/port`. Infrastructure implements those ports under `infrastructure/adapter/repository` and `infrastructure/adapter/model`. HTTP contracts live in `api`, while Controllers are inbound adapters under `trigger/http`. The app module only assembles the runtime. The domain does not depend on Spring, MyBatis, PostgreSQL, or AI SDKs.

An ArchUnit test in `kiln-ai-domain` prevents the domain from depending on Spring, MyBatis, JPA, or AI types.

## Prerequisites

- Java 21
- Docker Desktop

## Run Locally

1. Create local environment settings:

   ```bash
   cp deploy/local/.env.example deploy/local/.env
   ```

   Fill `OPENAI_API_KEY` in `deploy/local/.env` with an OpenAI-compatible API key. Startup and Docker Compose both read that file. Tests do not.

2. Start PostgreSQL:

   ```bash
   docker compose --env-file deploy/local/.env -f deploy/local/compose.yaml up -d
   ```

3. Run the application module:

   ```bash
   ./mvnw -pl kiln-ai-app -am spring-boot:run
   ```

Flyway applies the Learning Flow schema automatically. The learner UI is served at `http://localhost:8080/`.

## Try The Learning Flow

The learner UI at `/` walks through the whole flow. The same flow is available over the unified Learning Flow API:

Start a Diagnostic task:

```bash
curl -X POST http://localhost:8080/api/learning/flows \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: <uuid>' \
  -d '{"learnerId": "<uuid>"}'
```

Submit the Diagnostic answer (the response returns a fresh Independent Test on a neutral transition):

```bash
curl -X POST http://localhost:8080/api/learning/flows/<flowId>/commands \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: <uuid>' \
  -d '{"command": "answer_submitted", "interactionVersion": 1, "attemptId": "<attemptId>", "rawAnswer": "12x²−6x+7", "confirmedCanonical": "12*x^2-6*x+7", "rationale": null}'
```

Every learner command is one closed discriminator — `answer_submitted`, `hint_requested`, `clarification_asked`, `assistance_decided`, `continue_requested`, or `flow_control_requested` — and carries the expected `interactionVersion` (plus `attemptId` when it targets an open Attempt). Query the latest committed interaction at any time:

```bash
curl http://localhost:8080/api/learning/flows/<flowId>
```

The response exposes one closed committed-interaction union — `task`, `teaching`, `assistance_consent`, `transition`, or `unavailable` — with the closed command names the learner may issue against it. Due Review Tasks live under `/api/review-tasks`. Learner responses never contain expected answers, source identities, Fingerprints, or execution traces.

## Verification

```bash
./mvnw clean test
```

`ApplyProfileContractTest` is the stable regression oracle: it runs the whole Apply reference with scripted generation, Task Verification, Assessment, and Response Verification fixtures. No live model is called.

`ApplyProfileLiveSmokeTest` is a separate, non-blocking real-model smoke test. It is not a CI oracle and creates no evidence. To run it against the operator-configured model:

```bash
KILN_LIVE_SMOKE=true ./mvnw -pl kiln-ai-app -am test -Dtest=ApplyProfileLiveSmokeTest -Dsurefire.failIfNoSpecifiedTests=false
```
