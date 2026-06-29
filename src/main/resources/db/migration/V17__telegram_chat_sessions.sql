-- Сессии диалога Telegram-бота: связывает чат + проект с id сеанса claude (--resume).
-- Один активный (is_current) проект на чат; по сеансу на каждый (chat_id, project_name).
CREATE TABLE telegram_chat_sessions
(
    id                UUID         NOT NULL PRIMARY KEY,
    chat_id           BIGINT       NOT NULL,
    project_name      VARCHAR(255) NOT NULL,
    claude_session_id VARCHAR(255),
    is_current        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_telegram_chat_sessions_chat_project UNIQUE (chat_id, project_name)
);

-- Поиск активного проекта чата
CREATE INDEX idx_telegram_chat_sessions_current ON telegram_chat_sessions (chat_id, is_current);
