# Findings

## Initial Context

- README claims one full Learning/Practice reference path and says `/` walks through the whole flow.
- Normative stages are Diagnostic, Learning and Practice, Independent Test, and later Delayed Review.
- A passing Diagnostic is not Independent evidence; it only routes through a Neutral Transition to a fresh Independent Test.
- A failed Diagnostic should enter Explain, Apply Practice with Hint Ladder, Teach-back, then a fresh Independent Test.
- The worktree already contains uncommitted changes in model configuration, model adapter, persistence mapper, tests, environment example, and `index.html`; these must be preserved.

## Model And 503 Findings

- Fail-closed is intentional policy, but the committed baseline has no live Spring composition bean. The only real wiring is the currently untracked `OperatorModelConfiguration.java`; a clean checkout/deployment cannot call a model.
- The committed `.env.example` omitted both `kiln.catalog.enabled=true` and `kiln.catalog.output-token-ceiling`; the current uncommitted edit adds them. Missing `enabled` selects every fail-closed port and produces 503 on flow creation.
- The current local `.env` does contain the activation flag and catalog fields, so a 503 from the currently running server needs an HTTP/provider-level reproduction rather than being attributed to the original activation bug.
- Real provider failures, blank provider content, JSON contract parsing failures, and missing runtime secrets are all collapsed to `SERVICE_UNAVAILABLE`; the learner sees only 503 as a category.
- The GlobalExceptionHandler exposes exception messages, including upstream provider failure text, directly to the learner, and `index.html` prints the complete error body.
- Flow creation persists the Flow and source pack before model generation. A provider 503 can therefore leave a Flow without a committed interaction, violating the repository's stated generate-before-durable-mutation invariant.
- Answer submission closes an Attempt before model assessment. A provider 503 after closure can make replay return `ALREADY_SUBMITTED` rather than resume assessment, which is a serious recovery defect.
- The documented live smoke test creates placeholder model bindings/secret variable names instead of resolving the operator profile and therefore does not exercise the documented `.env` setup correctly.
- The local ignored `.env` contains a live-looking provider credential and is mode 0644. Do not reproduce it in output; recommend immediate rotation and owner-only file permissions.

## Current UI

- The current uncommitted `index.html` renders all five interaction kinds and gates buttons from `allowedEvents`.
- It supports Diagnostic, Apply Practice hints/H5, Explain Continue, assistance consent, Teach-back submission, Independent Test, and Review discovery/start through the same generic controls.
- The UI does not explain the current stage, purpose, mastery/evidence meaning, or why a Diagnostic pass leads to a second test. It dumps raw response JSON, so implemented stages are technically reachable but conceptually opaque.

## Live Reproduction

- Current `localhost:8080` runs from the local target classes, with PostgreSQL healthy.
- `POST /api/learning/flows` using a new learner and idempotency UUID succeeded with HTTP 201 and returned a generated Diagnostic task. This proves the current local catalog enabled real model generation; fail-closed is not the direct cause of every observed issue.
- Submitting a deliberately incorrect but canonical Diagnostic answer returned HTTP 503. The response reported that `ResponseAssessment` could not be constructed because its required `schema` was null. The live model output therefore failed the closed response-assessment contract, but the adapter categorised it as a provider outage.
- Replaying the same request returned HTTP 409 `ALREADY_SUBMITTED`. A subsequent GET still returns the old version-1 Diagnostic interaction and allows an answer submission even though its Attempt is closed. This directly reproduces an unrecoverable split between persisted Attempt state and learner-visible committed state.

## Additional Confirmed Gaps

- `AssessmentRunner` calls a model for a wrong Diagnostic expression, and `ApplyModelAdapter.parse` turns malformed/contract-invalid model JSON into `SERVICE_UNAVAILABLE`. Unlike generation profiles, assessment has no bounded repair or neutral Inconclusive outcome.
- `DiagnosticFlow` explicitly converts a recovered closed Diagnostic Attempt to `ALREADY_SUBMITTED`, contrary to ADR-0063's instruction to resume evaluation of a saved submission after an interrupted transition. The corresponding Independent/Test recovery route is implemented, creating inconsistent semantics by attempt purpose.
- A failing model call during `LearningFlowCommandUseCase.start` persists the Flow and source pack before first generation; the accepted ADR records the known orphan Flow outcome, but it conflicts with the global persistence invariant.
- The public API includes a learner-safe hint object, including H5 content when exposed, but `index.html` never renders `data.hint`. Thus H1-H5 are not usable from the reference UI despite working at the API level.
- Teach-back has a short-text task contract, but the page always presents mathematical raw/canonical answer inputs and auto-canonicalizes them. It has no dynamic rendering for `answerFields`, so its UI representation is misleading and does not honor the task contract.
- The page's retry button only remembers failed command requests, never failed flow starts. More importantly, a 503 after attempt closure cannot be retried successfully because the backend does not resume the Diagnostic assessment.
- API/UI tests use scripted model outputs and pass, but tests do not exercise model malformed assessment responses, live provider contract variance, 503 recovery, full UI hint rendering, H5 rendering, Teach-back text entry, consent interaction, or unavailable retry.

## Verification

- Focused configuration tests passed: `./mvnw -pl kiln-ai-app -am test -Dtest=FailClosedCatalogHttpTest,OperatorModelConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false` (4 tests).
- Focused API/UI tests passed: `./mvnw -pl kiln-ai-app -am test -Dtest=LearningFlowHttpTest,LearningFlowUiTest -Dsurefire.failIfNoSpecifiedTests=false` (11 tests).
- Full non-clean suite passed: `./mvnw test` (55 tests, 1 expected skipped live smoke test). This does not invalidate the live reproduction because the test fixtures bypass the live model.

## Follow-up Page Review

- The learner page is a generic command console rather than a Phase 0 flow view. It has no stage/progress presentation; `stage`, `attemptPurpose`, milestone, and evidence meaning are only visible in the raw JSON `<pre>`.
- All possible controls are permanently present and only disabled from `allowedEvents`, so the page does not explain why the learner is seeing a hint, consent, Continue, Independent Test, or Review action.
- `render()` never renders `data.hint`, despite the API mapper and DTO exposing it. H1-H5 content is therefore not a normal learner-visible section.
- The page always renders mathematical answer/canonical-expression inputs. Teach-back requires one short-text response, so the UI does not honor the returned `answerFields` contract.
- Transition and unavailable messages are inserted into the `#task` area, making a state transition look like a task instead of a distinct next-step/status boundary.
- The raw response JSON, UUIDs, enum names, and implementation fields are shown to learners as the main state view.
- `render()` re-enables `开始诊断` for every response, allowing a new Flow to overwrite the current in-memory Flow while the old Flow remains durable.
- Only `learnerId` survives reload; `flowId` is not persisted, so an active Flow cannot be resumed from the page after refresh.
- The page starts review work only after terminal state and has no learner-facing explanation of how Independent Evidence becomes Delayed Review cadence.
