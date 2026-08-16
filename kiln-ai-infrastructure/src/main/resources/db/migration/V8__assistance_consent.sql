-- Assistance-consent interactions for the Clarification Gate (ADR-0014): an
-- open Independent or Review attempt that received a substantive or uncertain
-- clarification pauses at an explicit assistance-consent request before any
-- help is exposed. The learner-visible consent projection is a first-class
-- interaction field, so the consent boundary survives restarts exactly like
-- teaching projections and hint views.

ALTER TABLE apply_interactions ADD COLUMN assistance_consent JSONB;
