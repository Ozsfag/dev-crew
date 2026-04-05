ALTER TABLE usage_records
    ADD CONSTRAINT uq_usage_records_task_id UNIQUE (task_id);
