-- V18: webhook_events table for idempotent billing event tracking.
-- The fixna-platform-common billing entities (Subscription, WebhookEvent)
-- are on the consulting component-scan path (in.fixna.platform), so Hibernate
-- validates the mapped webhook_events table at startup (ddl-auto: validate).
-- Consulting has no billing flows of its own; this table is inert
-- infrastructure for the shared entities. Never stores payment credentials.
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    error TEXT
);

CREATE INDEX idx_webhook_events_event_id ON webhook_events(event_id);
CREATE INDEX idx_webhook_events_processed_at ON webhook_events(processed_at DESC);
