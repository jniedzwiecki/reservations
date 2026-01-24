-- Create user_venue_assignments junction table
CREATE TABLE user_venue_assignments (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, venue_id)
);

CREATE INDEX idx_user_venue_user ON user_venue_assignments(user_id);
CREATE INDEX idx_user_venue_venue ON user_venue_assignments(venue_id);
