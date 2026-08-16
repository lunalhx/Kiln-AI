-- Learning Flow durable schema: the only product schema after the
-- destructive Learning Flow baseline. The old apply_* tables from the
-- Apply-only cutover were dropped and renamed without data migration; the
-- baseline persists Flow, Interaction, Checkpoint, Command, Attempt,
-- Evidence, and Review Task plus the artifact, exposure, hint, verification,
-- and assessment ledgers the guarded Learning StateGraph commits through.

CREATE TABLE flows (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE interactions (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES flows (id),
    interaction_version INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(40) NOT NULL,
    attempt_id UUID,
    attempt_purpose VARCHAR(30),
    learner_projection JSONB,
    learner_message TEXT,
    teaching_projection JSONB,
    hint JSONB,
    assistance_consent JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (flow_id, interaction_version)
);

CREATE TABLE checkpoints (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES flows (id),
    interaction_version INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE commands (
    idempotency_key UUID PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    flow_id UUID NOT NULL REFERENCES flows (id),
    response JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE sources (
    source_pack_id VARCHAR(200) PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    passages JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE packages (
    id UUID PRIMARY KEY,
    attempt_purpose VARCHAR(30) NOT NULL,
    learner_projection JSONB NOT NULL,
    private_assessor_projection JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- One open-or-closed Attempt per Task Package: the old apply_* baseline
-- already allowed the same plain-id reference to hold either an Apply package
-- or a teach-back package (the FK was dropped in the teach-back slice), and
-- the UNIQUE one-attempt-per-package invariant is preserved here.
CREATE TABLE attempts (
    id UUID PRIMARY KEY,
    task_package_id UUID NOT NULL UNIQUE,
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    submission JSONB,
    assistance_trace JSONB NOT NULL DEFAULT '[]'::jsonb
);

-- Verification records also reference packages that were rejected before
-- exposure and therefore never persisted as packages rows.
CREATE TABLE verifications (
    id UUID PRIMARY KEY,
    task_package_id UUID NOT NULL,
    verdict JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE assessments (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts (id),
    assessment JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE evidence (
    id UUID PRIMARY KEY,
    task_attempt_id UUID NOT NULL UNIQUE REFERENCES attempts (id),
    flow_id UUID NOT NULL REFERENCES flows (id),
    concept_id UUID NOT NULL,
    learner_id UUID NOT NULL,
    result VARCHAR(20) NOT NULL,
    attempt_purpose VARCHAR(30) NOT NULL,
    highest_hint_level SMALLINT NOT NULL,
    assistance_trace JSONB NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL
);

-- Exposure ledger for Task Packages and worked examples; the generated
-- content Fingerprints recorded here are the novelty exclusions of later
-- generation in the same Flow.
CREATE TABLE exposures (
    flow_id UUID NOT NULL REFERENCES flows (id),
    task_package_id UUID NOT NULL,
    task_fingerprint VARCHAR(255) NOT NULL,
    solution_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, task_package_id)
);

CREATE TABLE example_exposures (
    flow_id UUID NOT NULL REFERENCES flows (id),
    example_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, example_fingerprint)
);

CREATE TABLE hint_ladder_exposures (
    flow_id UUID NOT NULL REFERENCES flows (id),
    ladder_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, ladder_fingerprint)
);

CREATE TABLE revealed_solution_exposures (
    flow_id UUID NOT NULL REFERENCES flows (id),
    reveal_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, reveal_fingerprint)
);

CREATE TABLE explain_artifacts (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES flows (id),
    artifact JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hint_ladders (
    attempt_id UUID PRIMARY KEY REFERENCES attempts (id),
    ladder JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hint_requests (
    attempt_id UUID NOT NULL REFERENCES attempts (id),
    command_key UUID NOT NULL,
    requested_level SMALLINT NOT NULL,
    exposed_level SMALLINT NOT NULL,
    exposed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (attempt_id, command_key)
);

CREATE TABLE teach_back_anchors (
    flow_id UUID NOT NULL REFERENCES flows (id),
    anchor_id UUID NOT NULL,
    anchor_kind VARCHAR(40) NOT NULL,
    exposed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, anchor_id)
);

CREATE TABLE teach_back_packages (
    id UUID PRIMARY KEY,
    attempt_purpose VARCHAR(30) NOT NULL,
    learner_projection JSONB NOT NULL,
    private_projection JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE teach_back_assessments (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts (id),
    assessment JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Delayed Review cadence: durable Review Task coordination state. Review
-- Tasks are application-owned work items, never Task Packages; milestones are
-- projected only from evidence. At most one unfinished Review Task
-- (SCHEDULED, DUE, or STARTED) may exist per learner and Concept; the partial
-- unique index is the database-level concurrency guard behind the domain
-- invariant.
CREATE TABLE review_tasks (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL,
    flow_id UUID NOT NULL REFERENCES flows (id),
    review_number SMALLINT NOT NULL CHECK (review_number BETWEEN 1 AND 4),
    status VARCHAR(20) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    open_attempt_id UUID,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX review_tasks_one_unfinished_per_learner_concept
    ON review_tasks (learner_id, concept_id)
    WHERE status IN ('SCHEDULED', 'DUE', 'STARTED');
