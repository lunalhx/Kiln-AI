-- Phase 0 graph validation spike schema. V1 tables remain inert.

CREATE TABLE concept_contracts (
    id UUID PRIMARY KEY,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    version INT NOT NULL,
    name VARCHAR(200) NOT NULL,
    included_scope TEXT NOT NULL,
    excluded_scope TEXT NOT NULL,
    mastery_criterion TEXT NOT NULL,
    source_basis TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (concept_id, version)
);

CREATE TABLE mastery_rubrics (
    id UUID PRIMARY KEY,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    version INT NOT NULL,
    descriptors JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (concept_id, version)
);

CREATE TABLE artifacts (
    id UUID PRIMARY KEY,
    artifact_type VARCHAR(50) NOT NULL,
    schema_version INT NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE learning_flows (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    contract_id UUID NOT NULL REFERENCES concept_contracts (id),
    rubric_id UUID NOT NULL REFERENCES mastery_rubrics (id),
    source_pack_id UUID NOT NULL REFERENCES artifacts (id),
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE task_attempts (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES learning_flows (id),
    task_package_id UUID NOT NULL REFERENCES artifacts (id),
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ
);

CREATE TABLE accepted_learning_evidence (
    id UUID PRIMARY KEY,
    task_attempt_id UUID NOT NULL UNIQUE REFERENCES task_attempts (id),
    flow_id UUID NOT NULL REFERENCES learning_flows (id),
    concept_id UUID NOT NULL REFERENCES concepts (id),
    learner_id UUID NOT NULL,
    result VARCHAR(20) NOT NULL,
    attempt_purpose VARCHAR(30) NOT NULL,
    highest_hint_level SMALLINT NOT NULL,
    assistance_trace JSONB NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE concept_progress (
    learner_id UUID NOT NULL,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    current_milestone VARCHAR(20) NOT NULL,
    highest_milestone VARCHAR(20) NOT NULL,
    current_stage VARCHAR(40) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (learner_id, concept_id)
);

CREATE TABLE learner_interactions (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES learning_flows (id),
    interaction_version INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    stage VARCHAR(40) NOT NULL,
    visible_content TEXT NOT NULL,
    allowed_event_kinds JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (flow_id, interaction_version)
);

CREATE TABLE learning_checkpoints (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES learning_flows (id),
    thread_id VARCHAR(100) NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    next_node_id VARCHAR(100) NOT NULL,
    schema_version INT NOT NULL,
    blackboard JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE graph_run_traces (
    id UUID PRIMARY KEY,
    flow_id UUID NOT NULL REFERENCES learning_flows (id),
    schema_version INT NOT NULL,
    private_payload JSONB NOT NULL,
    public_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE processed_commands (
    idempotency_key UUID PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    flow_id UUID,
    status_code INT NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

INSERT INTO concepts (id, title, summary, source_reference, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Percent change',
    'Compute percent change between two positive quantities.',
    'fixture',
    TIMESTAMPTZ '2026-08-13 00:00:00Z'
);

INSERT INTO concept_contracts (
    id, concept_id, version, name, included_scope, excluded_scope, mastery_criterion, source_basis, created_at
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    1,
    'Percent change',
    'Percent increase or decrease from an old value to a new value.',
    'Compound growth, logs, and percent points.',
    'Independently compute percent change as (new - old) / old * 100.',
    'Prepared source pack v1',
    TIMESTAMPTZ '2026-08-13 00:00:00Z'
);

INSERT INTO mastery_rubrics (id, concept_id, version, descriptors, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    1,
    '{"criteria":["correct formula","correct arithmetic"]}',
    TIMESTAMPTZ '2026-08-13 00:00:00Z'
);

INSERT INTO artifacts (id, artifact_type, schema_version, visibility, content_hash, payload, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'SOURCE_PACK',
    1,
    'private',
    'fixture-source-pack',
    '{"excerpts":["Percent change = (new - old) / old * 100"]}',
    TIMESTAMPTZ '2026-08-13 00:00:00Z'
);
