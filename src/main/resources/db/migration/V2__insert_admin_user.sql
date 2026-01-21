-- Insert admin user
-- Email: admin@concerthall.com
-- Password: admin123 (BCrypt hash)
INSERT INTO users (id, email, password, role, is_removable, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin@concerthall.com',
    '$2a$10$7r6y2IxkKgTYcuWlZ//eFOUIEs.Bgo9/z.TcVdAvVRdwDRQ8vLY/G',
    'ADMIN',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
