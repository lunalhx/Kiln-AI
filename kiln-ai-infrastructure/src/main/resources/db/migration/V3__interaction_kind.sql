-- The closed learner interaction union of the unified Learning API: each
-- committed interaction declares whether it is a task, teaching,
-- assistance_consent, transition, or unavailable boundary. The kind column is
-- written on every new boundary; like V2, the DEFAULT covers any pre-existing
-- rows without a data migration.
ALTER TABLE interactions ADD COLUMN kind VARCHAR(30) NOT NULL DEFAULT 'transition';
