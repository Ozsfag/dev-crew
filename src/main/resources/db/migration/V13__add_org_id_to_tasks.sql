-- Денормализация: добавляем org_id в tasks для прямой фильтрации без JOIN через projects.
ALTER TABLE tasks ADD COLUMN org_id UUID;

-- Заполняем org_id из связанного проекта
UPDATE tasks SET org_id = p.org_id FROM projects p WHERE tasks.project_id = p.id;

-- Делаем NOT NULL после заполнения (задачи без проекта — допустимы, поэтому nullable)
-- ALTER TABLE tasks ALTER COLUMN org_id SET NOT NULL;

-- Индекс для быстрой фильтрации по организации
CREATE INDEX idx_tasks_org_id ON tasks(org_id);

-- FK на organizations
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_org_id FOREIGN KEY (org_id) REFERENCES organizations(id);
