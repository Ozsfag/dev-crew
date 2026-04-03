CREATE TABLE agents (
    id UUID PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    system_prompt TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    parent_task_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    assigned_to VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    result TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id)
);
