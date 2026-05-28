-- Demo tenant (self-loop: points to the openchat4u PG itself)
INSERT INTO tenants (name, code, description, db_type, jdbc_url, username, password, read_only, status, max_connections, connection_timeout, created_at, updated_at)
VALUES (
    'Demo Tenant',
    'demo',
    'Default demo tenant for first login',
    'POSTGRESQL',
    'jdbc:postgresql://localhost:5432/openchat4u',
    'postgres',
    'postgres',
    TRUE,
    'ACTIVE',
    5,
    10000,
    NOW(),
    NOW()
);
