---
status: accepted
---

# Give Action Skills exclusive model-draft field ownership

Each Profile call has exactly one Action Skill, and only that Skill's Manifest may declare fields in the model-generation draft. Capability and Subject Skills declare `output_contribution: []`; they constrain the Action's task construction and private facts but never merge independently generated values into the draft. The Profile still owns the final public/private artifact assembly. The compiler rejects a non-Action Bundle that claims a draft field. This prevents ambiguous field ownership while keeping reusable capabilities composable.
