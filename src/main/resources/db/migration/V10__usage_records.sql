CREATE TABLE usage_records
(
    id                UUID          NOT NULL PRIMARY KEY,
    task_id           UUID          NOT NULL,
    project_id        UUID,
    org_id            UUID          NOT NULL,
    agent_role        VARCHAR(50)   NOT NULL,
    prompt_tokens     INT           NOT NULL DEFAULT 0,
    completion_tokens INT           NOT NULL DEFAULT 0,
    cost_usd          DECIMAL(12, 8) NOT NULL DEFAULT 0,
    recorded_at       TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_usage_records_org_month ON usage_records (org_id, recorded_at);
