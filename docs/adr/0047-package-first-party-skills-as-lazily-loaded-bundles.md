---
status: accepted
---

# Package first-party Skills as lazily loaded Bundles

Every executable Kiln-AI Skill will live in a first-party versioned Skill Bundle containing short, always-loaded `SKILL.md` core instructions, machine-readable frontmatter for its Skill Manifest, declared lazy resources, and evaluation cases. The core contains only routine behavior that must apply to every execution; rare edge cases, extended examples, background rationale, and evaluation fixtures are resources rather than prompt bulk. Startup or registry publication reads only the frontmatter; after the deterministic Resolver freezes a Skill Stack, the Loader reads the selected Bundles' instructions and only the declared resources needed for that execution. This replaces the spike's hard-coded registry without allowing a model to browse the registry or execute external Skill files. External skills remain research references until explicitly rewritten and registered as first-party Bundles.
