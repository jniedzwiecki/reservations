-- Create venues table
CREATE TABLE venues (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    description TEXT,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_venues_name ON venues(name);
