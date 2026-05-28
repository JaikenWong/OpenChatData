-- OpenChat4U Initial Schema

CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    db_type VARCHAR(50) NOT NULL,
    jdbc_url VARCHAR(1000) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(1000) NOT NULL,
    read_only BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    max_connections INTEGER DEFAULT 5,
    connection_timeout INTEGER DEFAULT 10000,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE INDEX idx_tenants_code ON tenants(code);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    user_id BIGINT,
    user_ip VARCHAR(50),
    request_method VARCHAR(20),
    request_path VARCHAR(500),
    response_status INTEGER,
    execution_time_ms BIGINT,
    created_at TIMESTAMP
);
CREATE INDEX idx_audit_tenant ON audit_logs(tenant_code, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(tenant_code, action, created_at DESC);

CREATE TABLE query_history (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(255) NOT NULL,
    question VARCHAR(2000) NOT NULL,
    sql VARCHAR(4000),
    answer TEXT,
    result_count INTEGER,
    execution_time_ms BIGINT,
    is_success BOOLEAN,
    error_message VARCHAR(2000),
    user_id BIGINT,
    created_at TIMESTAMP
);
CREATE INDEX idx_history_tenant ON query_history(tenant_code, created_at DESC);

CREATE TABLE dictionaries (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    term VARCHAR(200) NOT NULL,
    synonyms VARCHAR(500),
    description VARCHAR(1000),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE INDEX idx_dict_tenant ON dictionaries(tenant_code, is_active);

CREATE TABLE data_masking_rules (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(255) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    mask_type VARCHAR(50) NOT NULL,
    mask_pattern VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);
CREATE INDEX idx_masking_tenant ON data_masking_rules(tenant_code, is_active);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    tenant_code VARCHAR(255),
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    action VARCHAR(50),
    created_at TIMESTAMP
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    tenant_code VARCHAR(255),
    created_at TIMESTAMP
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);

CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP
);
CREATE INDEX idx_role_perms_role ON role_permissions(role_id);
