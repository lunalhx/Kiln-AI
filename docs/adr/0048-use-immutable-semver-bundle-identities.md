---
status: accepted
---

# Use immutable SemVer Bundle identities

Each Skill Bundle has a stable semantic `id` and an immutable SemVer `version`; an Execution Plan pins both and the registry records a computed content hash for the complete selected Bundle. Existing pinned versions are never edited, so a revised instruction body, resource, or evaluation fixture is published as a new version rather than silently changing the behavior or audit trail of an earlier execution. Patch versions are limited to non-model-observable corrections, minor versions add compatible optional resources, strategies, or evaluation cases, and major versions change behavior boundaries, Context Requirements, Output Contributions, or compatibility.
