CREATE TABLE concepts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    source_reference TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE learner_concept_progress (
    user_id UUID NOT NULL,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    state VARCHAR(20) NOT NULL,
    has_independent_success BOOLEAN NOT NULL DEFAULT FALSE,
    has_delayed_independent_success BOOLEAN NOT NULL DEFAULT FALSE,
    has_transfer_success BOOLEAN NOT NULL DEFAULT FALSE,
    last_independent_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, concept_id),
    CONSTRAINT learner_concept_progress_state_check
        CHECK (state IN ('UNKNOWN', 'UNDERSTOOD', 'ASSISTED', 'INDEPENDENT', 'DURABLE'))
);

CREATE TABLE learning_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    event_type VARCHAR(30) NOT NULL,
    result VARCHAR(10) NOT NULL,
    hint_level SMALLINT NOT NULL,
    is_delayed_review BOOLEAN NOT NULL DEFAULT FALSE,
    is_transfer BOOLEAN NOT NULL DEFAULT FALSE,
    confidence SMALLINT,
    error_tag VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT learning_events_hint_level_check CHECK (hint_level BETWEEN 0 AND 4),
    CONSTRAINT learning_events_result_check CHECK (result IN ('PASS', 'PARTIAL', 'FAIL')),
    CONSTRAINT learning_events_confidence_check CHECK (confidence IS NULL OR confidence BETWEEN 1 AND 5)
);

CREATE TABLE review_tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    concept_id UUID NOT NULL REFERENCES concepts (id),
    task_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT review_tasks_status_check CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX learning_events_user_concept_occurred_at_idx
    ON learning_events (user_id, concept_id, occurred_at DESC);

CREATE INDEX learner_concept_progress_state_idx
    ON learner_concept_progress (user_id, state);

CREATE INDEX review_tasks_pending_due_at_idx
    ON review_tasks (user_id, due_at)
    WHERE status = 'PENDING';
