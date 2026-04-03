CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    details TEXT,
    timestamp TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_events_entity_id ON audit_events(entity_id);
