-- Frozen Model Profile snapshot copied onto a Learning Flow at start.

ALTER TABLE learning_flows
    ADD COLUMN frozen_profile JSONB NOT NULL DEFAULT '{}'::jsonb;
