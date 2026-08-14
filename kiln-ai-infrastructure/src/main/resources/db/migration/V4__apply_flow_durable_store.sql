-- Apply reference durable flow schema: typed LearningFlowStore + ArtifactStore.

CREATE TABLE apply_flows (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_interactions (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    interaction_version INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(40) NOT NULL,
    attempt_id UUID,
    attempt_purpose VARCHAR(30),
    learner_projection JSONB,
    learner_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (flow_id, interaction_version)
);

CREATE TABLE apply_checkpoints (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    interaction_version INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_exposures (
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    task_package_id UUID NOT NULL,
    task_fingerprint VARCHAR(255) NOT NULL,
    solution_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, task_package_id)
);

CREATE TABLE apply_commands (
    idempotency_key UUID PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    response JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_sources (
    source_pack_id VARCHAR(200) PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    passages JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_packages (
    id UUID PRIMARY KEY,
    attempt_purpose VARCHAR(30) NOT NULL,
    learner_projection JSONB NOT NULL,
    private_assessor_projection JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_attempts (
    id UUID PRIMARY KEY,
    task_package_id UUID NOT NULL UNIQUE REFERENCES apply_packages (id),
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    submission JSONB
);

-- Verification records also reference packages that were rejected before
-- exposure and therefore never persisted as apply_packages rows.
CREATE TABLE apply_verifications (
    id UUID PRIMARY KEY,
    task_package_id UUID NOT NULL,
    verdict JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_assessments (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES apply_attempts (id),
    assessment JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE apply_evidence (
    id UUID PRIMARY KEY,
    task_attempt_id UUID NOT NULL UNIQUE REFERENCES apply_attempts (id),
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    concept_id UUID NOT NULL,
    learner_id UUID NOT NULL,
    result VARCHAR(20) NOT NULL,
    attempt_purpose VARCHAR(30) NOT NULL,
    highest_hint_level SMALLINT NOT NULL,
    assistance_trace JSONB NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL
);
