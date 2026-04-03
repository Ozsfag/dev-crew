-- Привязка пользователей к организации
ALTER TABLE users
ADD COLUMN org_id UUID REFERENCES organizations(id) ON DELETE SET NULL;

CREATE INDEX idx_users_org_id ON users(org_id);
