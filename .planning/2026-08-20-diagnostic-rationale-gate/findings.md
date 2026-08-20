# Findings & Decisions

## Requirements
- A correct final answer remains sufficient for a Diagnostic pass.
- A proven-wrong final answer may still pass only when the learner's complete rationale demonstrates the required concept knowledge.
- A phrase blacklist is not acceptable: hedging language can coexist with a correct, substantive explanation.
- A rationale that is insufficient, unsupported, or semantically uncertain must not lead to Independent.
- Provider and model-contract failure must remain learner-safe `unavailable`/retry, not be treated as an inadequate learner rationale.

## Research Findings
- The reproduced submitted Diagnostic had a final expression equal to the original polynomial rather than its derivative and a rationale of “我不知道”. The persisted typed model assessment labeled the rationale `APPLICABLE`.
- `ResponseAssessmentDecider` intentionally maps a Diagnostic with `PROVEN_NOT_EQUIVALENT` final expression plus `APPLICABLE` rationale to `Passed`; the focused test asserts that behavior.
- The current Diagnostic flow also routes a generic `Inconclusive` outcome to Independent. The new policy must distinguish semantic rationale insufficiency from technical unavailability instead of relying on that generic branch.
- The `response_assessment/v1` JSON contract was valid in the reproduced event. Strict parsing rejects malformed shape or enum values, so JSON structure was not the cause.
- ADR-0057 established the original “substantive applicable rationale may pass a Diagnostic” policy. ADR-0005 and ADR-0045 already support selective isolated verification for consequential judgments.
- Current `ResponseVerificationPort` calls the same Strong model with the same `RESPONSE_ASSESSMENT_SYSTEM` prompt and the same context as the first assessment at judgment temperature 0.2. A verbatim second call is not a meaningful independent semantic review.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Preserve free-text rationale | The model must evaluate the whole explanation, including hedging and substantive content together. |
| Invoke Response Verification only after a proven-wrong Diagnostic final answer receives a first `APPLICABLE` assessment verdict | This is the narrow, consequential override path; correct answers and plainly inadequate rationales retain their current low-cost path. |
| Use a distinct, adversarial rationale-verification prompt and require both calls to return `APPLICABLE` | The verifier must independently try to falsify applicability from the full rationale rather than repeat the same classification prompt. |
| Keep the frozen Strong binding for the initial fix | Adding a third provider/model binding broadens the operator Model Profile; prompt-role separation is the smallest correct improvement now. |
| Treat `NOT_APPLICABLE`, `NON_SUBSTANTIVE`, `INCONCLUSIVE`, or assessment/verification disagreement as diagnostic remediation-required | None proves readiness for a fresh Independent test. Diagnostic creates no negative Evidence. |
| Preserve `unavailable` for provider/model-contract failure | System incapacity must not be interpreted as learner insufficiency. |
| Keep `CANNOT_DECIDE` final-expression behavior out of this change | The requested rule concerns a final answer already proven wrong; broader uncertainty policy needs separate product review. |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Existing `Inconclusive` has two meanings: semantic uncertainty and technical model failure | The implementation plan must introduce or preserve a typed distinction at the Diagnostic routing seam. |

## Resources
- `docs/adr/0005-separate-assessment-with-selective-verification.md`
- `docs/adr/0045-combine-proof-bounded-math-checks-with-isolated-model-assessment.md`
- `docs/adr/0057-separate-apply-final-expression-and-rationale-assessment-channels.md`
- `kiln-ai-domain/src/main/java/cn/lunalhx/ai/kilnai/domain/apply/model/ResponseAssessmentDecider.java`
- `kiln-ai-domain/src/main/java/cn/lunalhx/ai/kilnai/domain/apply/flow/AssessmentRunner.java`
- `kiln-ai-domain/src/main/java/cn/lunalhx/ai/kilnai/domain/apply/flow/DiagnosticFlow.java`
- `kiln-ai-infrastructure/src/main/java/cn/lunalhx/ai/kilnai/infrastructure/adapter/model/ApplyModelAdapter.java`
