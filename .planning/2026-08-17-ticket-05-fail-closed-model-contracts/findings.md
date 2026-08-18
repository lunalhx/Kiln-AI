# Findings

- Adapter currently parses Task Verification, Assessment, Teach-back Assessment, and Clarification with `FAIL_ON_UNKNOWN_PROPERTIES` disabled and maps parse failures to `SERVICE_UNAVAILABLE` (503).
- Generation already returns raw text; Domain parses via `ApplyGenerationDraft.parse`.
- Assessment has no repair loop; invalid JSON would surface as 503 rather than Inconclusive.
- `ApplyProfileExecutor` treats any `SERVICE_UNAVAILABLE` as provider unavailable, so a contract error is misclassified today.
- Scripted ports return typed objects; whole-flow invalid-contract tests will throw `ModelContractInvalidException` at the typed port, matching Domain parse failure.
