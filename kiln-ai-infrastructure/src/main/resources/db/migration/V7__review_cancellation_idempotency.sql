-- Explicit Review cancellation has its own idempotency ledger. It is not a
-- Learning Flow command and therefore never shares the commands discriminator
-- or its response records.
CREATE TABLE review_cancellation_commands (
    idempotency_key UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES review_tasks (id),
    request_hash VARCHAR(64) NOT NULL,
    response JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
