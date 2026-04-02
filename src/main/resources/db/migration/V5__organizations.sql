-- Организации (тенанты) и проекты внутри организации

CREATE TABLE organizations
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    plan       VARCHAR(50)  NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE projects
(
    id          UUID PRIMARY KEY,
    org_id      UUID         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    repo_path   VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_projects_org_id ON projects (org_id);
