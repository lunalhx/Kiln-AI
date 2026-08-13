# Findings: Spec readiness audit

## Confirmed repository facts

- The repository is a Java 21 / Maven multi-module draft using Spring Boot 4.0.7.
- Existing modules include `kiln-ai-types`, `kiln-ai-domain`, `kiln-ai-infrastructure`, `kiln-ai-api`, `kiln-ai-trigger`, and `kiln-ai-app`.
- Existing tests include domain unit/architecture tests and a Spring application-context test.
- Existing HTTP/application entry points are `ConceptController`, `LearningController`, and `LearningCommandService`; these are draft implementation artifacts rather than accepted interfaces.
- The user explicitly said the runnable code skeleton is only a draft and may be redesigned, so the current module layout and dependencies are not accepted product or architecture decisions.
- The repository contains accepted ADRs under `docs/adr`, a first-tracer plan, a framework-evaluation spike, and the root glossary/context document.
- The current ADR set is continuously numbered `0001` through `0033`; the spike is intentionally unnumbered and separate from accepted ADRs.
- No issue-tracker configuration or git remote was found in the initial audit; `git remote -v` returned no remote.

## Confirmed decision themes from the conversation and docs

- Phase 0 prioritizes building a usable product loop, not proving product efficacy.
- The orchestration design uses a Learning StateGraph, a deterministic Workflow Guard, one bounded Pedagogy Agent, and five Teaching Node Profiles.
- The five Teaching Node Profiles are Explain, Retrieve, Apply, Teach-back, and Hint; they are not independent autonomous agents.
- Per-flow typed blackboard state is shared through least-privilege node context views; Phase 0 excludes cross-flow learner memory.
- Skills are small, composable, capability-oriented methods selected through a deterministic manifest resolver and loaded progressively.
- Source content is separated from teaching-method skills through a format-neutral normalized source document and a Concept Source Pack.
- The first tracer uses a manually prepared normalized source fixture; permanent PDF/Markdown adapters and automated whole-textbook extraction are deferred.
- Typed artifact validation, deterministic gates/reducers, checkpointing, bounded repair, evidence, hint, independent-test, and model-call-budget rules are accepted.
- The first tracer's accepted completion evidence is a non-hardcoded prepared calculus Concept completing an adaptive Explain/Apply/Hint path, interrupt/resume, assistance-isolated assessment, and verified fresh-task advancement to Independent with complete traces.
- The framework spike is explicitly `Planned` and says not to adopt or implement it until implementation is explicitly requested.
- Spring AI Alibaba Graph is accepted only if all five hard gates pass: domain isolation, routing correctness, progressive Skill loading, reliable recovery, and testability/observability; failure of any gate selects the application-owned Java transition-engine fallback.
- ADR-0011 explicitly says adopting a Java graph library is a separate implementation decision; domain graph and transition semantics must remain application-owned and framework-independent.
- ADR-0032 deliberately leaves exact Token, cost, and latency ceilings to measurement-calibrated configuration while fixing the model-call ceilings and safe overflow behavior.
- ADR-0033 deliberately keeps the permanent textbook, edition, Target Concept, and input adapter open while requiring a manual normalized-source fixture for the first tracer.

## Testing seams present in accepted documents

- The framework spike requires graph transitions to be testable with model nodes replaced by deterministic fakes.
- The first tracer defines a learner-visible completion path but does not select one canonical automated seam or state whether the acceptance proof should run through HTTP, an application service, or a graph port.
- The current HTTP path only records already-produced learning evidence; it does not exercise the accepted graph, Skills, checkpoints, learner interactions, task generation, assessment, or gates.
- The current Spring application test is only an empty module-presence test, while the domain architecture test usefully enforces framework isolation. Neither is the end-to-end tracer seam required by the new design.

## Material unresolved candidates

- The scope of the requested implementation spec is not explicit: the whole Phase 0, the first tracer bullet, or the Spring AI Alibaba Graph evaluation spike.
- The graph runtime is deliberately unresolved until the five-gate spike is run; fallback is an application-owned lightweight Java transition engine.
- Implementation of the spike itself has not been explicitly chosen as the next spec scope, and implementation of the product tracer cannot honestly name a graph adapter until the spike resolves the runtime choice.
- The current draft's module decomposition, persistence vendor, and API/UI shape are not accepted decisions.
- The concrete model provider/model configuration is not selected.
- The university calculus textbook, source format, and first Target Concept remain intentionally unset; a concrete tracer fixture still needs values before an executable acceptance scenario can be fixed.
- The `to-spec` workflow requires publishing to a project tracker with `ready-for-agent`, but no tracker or label vocabulary is configured in the repository.
- A focused hidden-file search confirmed there is no repository tracker configuration, issue template, or `ready-for-agent` label reference; the repository also has no git remote.
- The repository has several possible test seams, but no single user-approved end-to-end seam has yet been selected for this spec.

## User resolutions

- Spec scope: Spring AI Alibaba Graph validation spike only.
- Sequence: complete the spike before fixing the first tracer's graph runtime implementation.
- Primary acceptance seam: the complete learner-facing HTTP/UI flow.
- Publication target: GitHub repository `lunalhx/Kiln-AI`.

## Publication status

- GitHub CLI 2.96.0 is installed.
- The configured `lunalhx` account is active but its stored token is invalid.
- The first repository query also hit the sandbox's restricted network path; retry with approved network access after the spec is ready.
- Available connector/API discovery returned no GitHub tool, so browser publication is the remaining authenticated fallback if CLI reauthentication is unavailable.
- The existing Chrome GitHub session was authenticated as repository owner and could access `lunalhx/Kiln-AI`.
- Created the missing `ready-for-agent` label with description `Implementation-ready specification`.
- Published the spec as GitHub Issue #1: `https://github.com/lunalhx/Kiln-AI/issues/1`.

## Open items that are not architecture blockers by themselves

- Exact Token, cost, and latency ceilings are intentionally runtime configuration to be calibrated by measurement; the accepted call ceilings and overflow behavior are sufficient to preserve the architecture.
- The permanent source adapter and textbook/Concept variables are intentionally decoupled from reusable contracts. They do not block framework-independent design, but a concrete manual fixture is still required to make the first tracer's acceptance scenario executable.

## Errors and recovery notes

- A root-level `find src ...` command failed because sources live inside Maven modules. Future inspection should target module paths or use `rg --files`.
- One broad parallel file-reading command produced truncated output. Continue with smaller, focused reads.
