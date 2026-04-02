-- Привязка задач к проекту (тенант-изоляция)
ALTER TABLE tasks
    ADD COLUMN project_id UUID REFERENCES projects (id) ON DELETE SET NULL;

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
