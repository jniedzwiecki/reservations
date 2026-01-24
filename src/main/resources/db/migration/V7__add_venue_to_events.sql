-- Add venue_id column to events table
ALTER TABLE events ADD COLUMN venue_id UUID REFERENCES venues(id) ON DELETE RESTRICT;

-- Create index for venue_id
CREATE INDEX idx_events_venue_id ON events(venue_id);

-- Create default venue for existing events
INSERT INTO venues (id, name, address, description, capacity)
VALUES (
    gen_random_uuid(),
    'Main Hall',
    'Default Location',
    'Default venue for migrated events',
    1000
);

-- Migrate existing events to default venue
UPDATE events
SET venue_id = (SELECT id FROM venues WHERE name = 'Main Hall' LIMIT 1)
WHERE venue_id IS NULL;

-- Make venue_id NOT NULL
ALTER TABLE events ALTER COLUMN venue_id SET NOT NULL;
