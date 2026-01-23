-- Remove the old unique constraint that includes all statuses
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS unique_user_event;

-- Create a partial unique index that only applies to active tickets (not cancelled or failed)
CREATE UNIQUE INDEX unique_active_user_event
ON tickets (user_id, event_id)
WHERE status IN ('PENDING_PAYMENT', 'PAID', 'RESERVED');

-- This allows users to have multiple cancelled/failed tickets for the same event,
-- but only one active ticket at a time
