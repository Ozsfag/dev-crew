-- Привязка событий аудита к проекту (тенант-изоляция)
ALTER TABLE audit_events
    ADD COLUMN project_id UUID REFERENCES projects (id) ON DELETE SET NULL;

CREATE INDEX idx_audit_events_project_id ON audit_events (project_id);
