-- Insert admin user
-- Email: admin@concerthall.com
-- Password: admin123 (BCrypt hash)
INSERT INTO users (email, password, role, is_removable, created_at, updated_at)
VALUES (
    'admin@concerthall.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/IVI6Z5ygLZ3r7tC4LeZYyX3GhNNKRe',
    'ADMIN',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
