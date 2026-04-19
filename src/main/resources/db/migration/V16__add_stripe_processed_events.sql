CREATE TABLE stripe_processed_event
(
  event_id     VARCHAR(255)             NOT NULL PRIMARY KEY,
  processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
