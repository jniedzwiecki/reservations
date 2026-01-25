-- Add source tracking to venues
ALTER TABLE venues
ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'INTERNAL'
CHECK (source IN ('INTERNAL', 'EXTERNAL_PROVIDER'));

ALTER TABLE venues
ADD COLUMN external_id VARCHAR(255);

CREATE INDEX idx_venues_external_id ON venues(external_id);
CREATE INDEX idx_venues_source ON venues(source);

-- Add external tracking to events
ALTER TABLE events
ADD COLUMN external_id VARCHAR(255);

CREATE INDEX idx_events_external_id ON events(external_id);

-- Add external reservation tracking to tickets
ALTER TABLE tickets
ADD COLUMN external_reservation_id VARCHAR(255);

ALTER TABLE tickets
ADD COLUMN external_confirmation_code VARCHAR(255);

CREATE INDEX idx_tickets_external_reservation ON tickets(external_reservation_id);

-- Add configuration table for external providers
CREATE TABLE external_provider_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_name VARCHAR(100) NOT NULL UNIQUE,
    api_base_url TEXT NOT NULL,
    api_key TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    rate_limit_per_hour INTEGER DEFAULT 1000,
    timeout_seconds INTEGER DEFAULT 30,
    retry_attempts INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_external_provider_config_enabled ON external_provider_config(enabled);

-- Insert default external provider config (disabled by default)
INSERT INTO external_provider_config (provider_name, api_base_url, api_key, enabled)
VALUES ('ExternalVenues', 'https://api.external-venues.com/v1', 'your-api-key-here', false);
