# Learning/Practice spec decision audit

## Goal

Identify what ADR-0026/0020/0014 and the current repository have already decided about the Learning/Practice phase, surface only implementation-significant unresolved product decisions, confirm the highest practical contract-test seam, and publish a spec only after the user confirms the remaining decisions.

## Phases

- [completed] Read required architecture, glossary, specs, tickets, code, and tests.
- [completed] Separate binding decisions from gaps and contradictions.
- [completed] Grill one implementation-significant decision at a time; update glossary/ADR only when a domain term or qualifying architectural trade-off is actually resolved.
- [completed] Confirm the proposed test seam with the user.
- [in_progress] Publish the completed local spec with the `ready-for-agent` label.

## Constraints

- Do not invent product behavior to make the spec look complete.
- Do not implement the feature during the design session.
- Use the vocabulary and boundaries from `CONTEXT.md`.
- Treat existing ADRs and normative specs/tickets as binding unless an explicit conflict is raised.

## Errors Encountered

- One planning-record patch failed because section order in `findings.md` differed from the assumed order. Read the actual headings and applied a targeted patch; no product or source file was affected.
- A second combined patch failed on another planning-record context mismatch. Verified that no ADR/source change was partially applied, then split the source and planning updates into exact patches.
- A third long-range combined patch hit the same context-matching class of error. Switched permanently to small per-file/per-section patches for the remaining design session.
