-- Add new payment-related statuses to ticket status constraint
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check;
ALTER TABLE tickets ADD CONSTRAINT tickets_status_check
    CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED', 'RESERVED', 'CANCELLED'));

-- Add payment expiration timestamp column
ALTER TABLE tickets ADD COLUMN payment_expires_at TIMESTAMP;

-- Create index for efficient expiration queries
CREATE INDEX idx_tickets_payment_expires ON tickets(payment_expires_at)
    WHERE status = 'PENDING_PAYMENT';

-- Migrate existing RESERVED tickets to PAID status
UPDATE tickets SET status = 'PAID' WHERE status = 'RESERVED';
