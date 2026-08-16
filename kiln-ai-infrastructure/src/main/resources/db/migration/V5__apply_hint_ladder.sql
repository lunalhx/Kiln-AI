-- Hint Ladder persistence for the Learning/Practice reference: the learner
-- interaction carries the exposed Hint View, attempts carry their append-only
-- Assistance Trace, and the private stable ladder plus the request records
-- live in their own tables so later requests reveal persisted levels without
-- another model call and a crashed command resumes its original exposure.

ALTER TABLE apply_interactions ADD COLUMN hint JSONB;

ALTER TABLE apply_attempts ADD COLUMN assistance_trace JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE TABLE apply_hint_ladders (
    attempt_id UUID PRIMARY KEY REFERENCES apply_attempts (id),
    ladder JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_hint_requests (
    attempt_id UUID NOT NULL REFERENCES apply_attempts (id),
    command_key UUID NOT NULL,
    requested_level SMALLINT NOT NULL,
    exposed_level SMALLINT NOT NULL,
    exposed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (attempt_id, command_key)
);
