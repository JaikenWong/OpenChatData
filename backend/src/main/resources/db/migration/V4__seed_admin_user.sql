-- Default admin user (password: admin123)
-- Bound to seeded demo tenant from V2.
INSERT INTO users (username, password_hash, tenant_code, display_name, status, login_attempts, created_at, updated_at)
VALUES (
    'admin',
    '$2a$10$iRy9I1nsTTY3ta5gMP35MebXJDBRKuAb8cROzNK1OTlclfXfQkMgC',
    'demo',
    'Default Admin',
    'ACTIVE',
    0,
    NOW(),
    NOW()
);
