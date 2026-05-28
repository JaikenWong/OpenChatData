CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    tenant_code VARCHAR(255) NOT NULL,
    display_name VARCHAR(200),
    email VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (username, tenant_code)
);
CREATE INDEX idx_users_username_tenant ON users(username, tenant_code);
