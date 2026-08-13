---
status: accepted
---

# Compose Skill Stacks through bounded Slots

Phase 0 will compile every Skill Stack through five named Slots: exactly one `action` Skill plus optional `reasoning`, `representation`, `verification`, and `subject` Skills, with at most one Skill per Slot. Manifests declare Slot ownership, and deterministic selection uses applicability and explicit priority; an equal-priority collision is rejected as registry misconfiguration. Profile permissions and the base envelope remain authoritative, the Action Skill owns pedagogical behavior, Capability Skills contribute only within their named reasoning, representation, or verification sections, and the Subject Skill adds domain terminology, constraints, conventions, or tools without overriding the Action method. A deterministic Prompt Compiler assembles namespaced sections rather than relying on last-instruction-wins concatenation, enforces per-Profile core-instruction and lazy-resource Token budgets before the model call, and returns Capability Gap instead of truncating a Skill. Additional Slots require a later demonstrated capability dimension rather than allowing unbounded composition.
