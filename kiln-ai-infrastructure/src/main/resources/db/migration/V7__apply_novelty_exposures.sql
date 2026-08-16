-- Novelty ledger for generated Hint Ladders and H5 revealed solutions: their
-- deterministic content fingerprints are recorded per Flow so later task and
-- example generation never reuses exposed hint content or the revealed answer.

CREATE TABLE apply_hint_ladder_exposures (
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    ladder_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, ladder_fingerprint)
);

CREATE TABLE apply_revealed_solution_exposures (
    flow_id UUID NOT NULL REFERENCES apply_flows (id),
    reveal_fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (flow_id, reveal_fingerprint)
);
