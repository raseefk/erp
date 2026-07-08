-- ============================================================
--  MANUAL SCHEMA SETUP FOR SUPER ERP ORACLE
--  Run this script ONCE before starting the application
--  This creates all required tables, indexes, and sequences
-- ============================================================

-- ── TENANTS TABLE ──────────────────────────────────────────
CREATE TABLE tenants (
    id                   RAW(16) PRIMARY KEY,
    slug                 VARCHAR2(63) NOT NULL UNIQUE,
    name                 VARCHAR2(255) NOT NULL,
    logo_url             CLOB,
    primary_color        VARCHAR2(20) DEFAULT '#3b82f6',
    is_active            NUMBER(1) DEFAULT 1,
    plan                 VARCHAR2(50) DEFAULT 'STANDARD',
    max_users            NUMBER(10) DEFAULT 10,
    max_storage_gb       NUMBER(10,2),
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    expires_at           TIMESTAMP,
    created_by           NUMBER(19)
);
CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_is_active ON tenants(is_active);
COMMIT;

-- ── APP USERS TABLE ───────────────────────────────────────
CREATE TABLE app_users (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    username             VARCHAR2(100) NOT NULL,
    password             VARCHAR2(255) NOT NULL,
    full_name            VARCHAR2(255) NOT NULL,
    email                VARCHAR2(255),
    is_enabled           NUMBER(1) DEFAULT 1,
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    UNIQUE (tenant_id, username)
);
CREATE INDEX idx_app_users_tenant_id ON app_users(tenant_id);
CREATE INDEX idx_app_users_username ON app_users(username);
CREATE INDEX idx_app_users_email ON app_users(email);
COMMIT;

-- ── APP ROLES TABLE ───────────────────────────────────────
CREATE TABLE app_roles (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    name                 VARCHAR2(100) NOT NULL,
    display_name         VARCHAR2(255),
    description          CLOB,
    is_system_role       NUMBER(1) DEFAULT 0,
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    UNIQUE (tenant_id, name)
);
CREATE INDEX idx_app_roles_tenant_id ON app_roles(tenant_id);
CREATE INDEX idx_app_roles_is_system ON app_roles(is_system_role);
COMMIT;

-- ── USER-ROLE MAPPING ─────────────────────────────────────
CREATE TABLE user_roles (
    user_id              NUMBER(19) NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id              NUMBER(19) NOT NULL REFERENCES app_roles(id),
    PRIMARY KEY (user_id, role_id)
);
COMMIT;

-- ── FEATURES TABLE ────────────────────────────────────────
CREATE TABLE features (
    id                   VARCHAR2(60) PRIMARY KEY,
    display_name         VARCHAR2(255) NOT NULL,
    description          CLOB,
    icon                 VARCHAR2(100),
    sort_order           NUMBER(10) DEFAULT 0
);
COMMIT;

-- ── PERMISSIONS TABLE ─────────────────────────────────────
CREATE TABLE permissions (
    id                   VARCHAR2(100) PRIMARY KEY,
    feature_id           VARCHAR2(60) NOT NULL REFERENCES features(id),
    display_name         VARCHAR2(255) NOT NULL,
    description          CLOB,
    action               VARCHAR2(50) NOT NULL
);
CREATE INDEX idx_permissions_feature_id ON permissions(feature_id);
COMMIT;

-- ── ROLE-PERMISSION MAPPING ───────────────────────────────
CREATE TABLE role_permissions (
    role_id              NUMBER(19) NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    permission_id        VARCHAR2(100) NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);
COMMIT;

-- ── TENANT FEATURE MAPPING ────────────────────────────────
CREATE TABLE tenant_feature_mappings (
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    feature_id           VARCHAR2(60) NOT NULL REFERENCES features(id),
    is_enabled           NUMBER(1) DEFAULT 1,
    PRIMARY KEY (tenant_id, feature_id)
);
CREATE INDEX idx_tenant_features_tenant_id ON tenant_feature_mappings(tenant_id);
COMMIT;

-- ── COMPANY SETTINGS TABLE ────────────────────────────────
CREATE TABLE company_settings (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL UNIQUE REFERENCES tenants(id),
    company_name         VARCHAR2(255) NOT NULL,
    registration_number  VARCHAR2(100),
    industry             VARCHAR2(100),
    country              VARCHAR2(100),
    state_province       VARCHAR2(100),
    city                 VARCHAR2(100),
    address              VARCHAR2(500),
    postal_code          VARCHAR2(20),
    phone_number         VARCHAR2(20),
    email                VARCHAR2(255),
    website              VARCHAR2(255),
    tax_id               VARCHAR2(50),
    bank_name            VARCHAR2(255),
    bank_account_number  VARCHAR2(100),
    bank_code            VARCHAR2(20),
    accounting_year_end  VARCHAR2(10),
    financial_year_start VARCHAR2(10),
    currency             VARCHAR2(3) DEFAULT 'USD',
    logo_url             CLOB,
    description          CLOB,
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE
);
CREATE INDEX idx_company_settings_tenant_id ON company_settings(tenant_id);
COMMIT;

-- ── AUDIT LOG TABLE ───────────────────────────────────────
CREATE TABLE audit_logs (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    user_id              NUMBER(19) REFERENCES app_users(id),
    action               VARCHAR2(100) NOT NULL,
    entity_type          VARCHAR2(100) NOT NULL,
    entity_id            VARCHAR2(100),
    old_values           CLOB,
    new_values           CLOB,
    timestamp            TIMESTAMP DEFAULT SYSDATE,
    ip_address           VARCHAR2(45),
    user_agent           VARCHAR2(500)
);
CREATE INDEX idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
COMMIT;

-- ── TOKEN BLACKLIST TABLE ─────────────────────────────────
CREATE TABLE token_blacklist (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    token                CLOB NOT NULL,
    token_hash           VARCHAR2(255) UNIQUE NOT NULL,
    user_id              NUMBER(19) REFERENCES app_users(id),
    blacklisted_at       TIMESTAMP DEFAULT SYSDATE,
    expires_at           TIMESTAMP NOT NULL
);
CREATE INDEX idx_token_blacklist_user_id ON token_blacklist(user_id);
CREATE INDEX idx_token_blacklist_expires_at ON token_blacklist(expires_at);
COMMIT;

-- ── CUSTOMERS TABLE ───────────────────────────────────────
CREATE TABLE customers (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    customer_code        VARCHAR2(50) NOT NULL,
    name                 VARCHAR2(255) NOT NULL,
    email                VARCHAR2(255),
    phone                VARCHAR2(20),
    address              VARCHAR2(500),
    city                 VARCHAR2(100),
    state                VARCHAR2(100),
    postal_code          VARCHAR2(20),
    country              VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    UNIQUE (tenant_id, customer_code)
);
CREATE INDEX idx_customers_tenant_id ON customers(tenant_id);
COMMIT;

-- ── VENDORS TABLE ─────────────────────────────────────────
CREATE TABLE vendors (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    vendor_code          VARCHAR2(50) NOT NULL,
    name                 VARCHAR2(255) NOT NULL,
    email                VARCHAR2(255),
    phone                VARCHAR2(20),
    address              VARCHAR2(500),
    city                 VARCHAR2(100),
    state                VARCHAR2(100),
    postal_code          VARCHAR2(20),
    country              VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    UNIQUE (tenant_id, vendor_code)
);
CREATE INDEX idx_vendors_tenant_id ON vendors(tenant_id);
COMMIT;

-- ── INVENTORY ITEMS TABLE ────────────────────────────────
CREATE TABLE inventory_items (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    item_code            VARCHAR2(50) NOT NULL,
    name                 VARCHAR2(255) NOT NULL,
    description          CLOB,
    quantity_on_hand     NUMBER(10,2) DEFAULT 0,
    reorder_level        NUMBER(10,2) DEFAULT 0,
    unit_cost            NUMBER(15,2) DEFAULT 0,
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    UNIQUE (tenant_id, item_code)
);
CREATE INDEX idx_inventory_items_tenant_id ON inventory_items(tenant_id);
COMMIT;

-- ── EMPLOYEES TABLE ───────────────────────────────────────
CREATE TABLE employees (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    employee_code        VARCHAR2(50) NOT NULL,
    first_name           VARCHAR2(100) NOT NULL,
    last_name            VARCHAR2(100),
    email                VARCHAR2(255),
    phone                VARCHAR2(20),
    department           VARCHAR2(100),
    designation          VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT SYSDATE,
    updated_at           TIMESTAMP DEFAULT SYSDATE,
    UNIQUE (tenant_id, employee_code)
);
CREATE INDEX idx_employees_tenant_id ON employees(tenant_id);
COMMIT;

COMMIT;
PROMPT Schema creation completed successfully!
