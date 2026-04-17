-- Добавляем ссылку на пользователя вместо хранения только email.
-- actor_email оставляем для обратной совместимости.
ALTER TABLE audit_events ADD COLUMN actor_id UUID;

-- FK на users (ON DELETE SET NULL — при удалении пользователя история сохраняется)
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_actor_id
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL;

-- Индекс для фильтрации по пользователю
CREATE INDEX idx_audit_events_actor_id ON audit_events(actor_id);
