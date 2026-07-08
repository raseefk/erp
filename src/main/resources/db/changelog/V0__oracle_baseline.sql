-- ============================================================
--  V0: Oracle Baseline Schema for Single-Tenant ERP
--  Converts from PostgreSQL multi-tenant to Oracle single-tenant
--  All tenant_id columns retained for schema compatibility
-- ============================================================

-- ── TENANTS (single app tenant record) ──────────────────────
CREATE TABLE tenants (
    id                   RAW(16) PRIMARY KEY,
    slug                 VARCHAR2(63) NOT NULL UNIQUE,
    name                 VARCHAR2(255) NOT NULL,
    logo_url             CLOB,
    primary_color        VARCHAR2(20) DEFAULT '#3b82f6' NOT NULL,
    is_active            NUMBER(1) DEFAULT 1 NOT NULL,
    plan                 VARCHAR2(50) DEFAULT 'STANDARD' NOT NULL,
    max_users            NUMBER(10) DEFAULT 10 NOT NULL,
    created_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    updated_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    expires_at           TIMESTAMP,
    created_by           NUMBER(19)
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_is_active ON tenants(is_active);

-- ── SYSTEM USERS (for legacy compatibility, minimal use) ─────
CREATE TABLE system_users (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    username             VARCHAR2(100) NOT NULL UNIQUE,
    password             VARCHAR2(255) NOT NULL,
    full_name            VARCHAR2(255) NOT NULL,
    email                VARCHAR2(255) NOT NULL UNIQUE,
    is_enabled           NUMBER(1) DEFAULT 1 NOT NULL,
    created_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    updated_at           TIMESTAMP DEFAULT SYSDATE NOT NULL
);

CREATE INDEX idx_system_users_username ON system_users(username);

-- ── FEATURES (application-level modules) ────────────────────
CREATE TABLE features (
    id                   VARCHAR2(60) PRIMARY KEY,
    display_name         VARCHAR2(255) NOT NULL,
    description          CLOB,
    icon                 VARCHAR2(100),
    sort_order           NUMBER(10) DEFAULT 0 NOT NULL
);

-- ── MENUS (pages within features) ────────────────────────────
CREATE TABLE menus (
    id                   VARCHAR2(60) PRIMARY KEY,
    feature_id           VARCHAR2(60) NOT NULL REFERENCES features(id),
    display_name         VARCHAR2(255) NOT NULL,
    url_pattern          CLOB,
    icon                 VARCHAR2(100),
    sort_order           NUMBER(10) DEFAULT 0 NOT NULL
);

CREATE INDEX idx_menus_feature_id ON menus(feature_id);

-- ── PERMISSIONS (granular actions) ───────────────────────────
CREATE TABLE permissions (
    id                   VARCHAR2(100) PRIMARY KEY,
    feature_id           VARCHAR2(60) NOT NULL REFERENCES features(id),
    menu_id              VARCHAR2(60) REFERENCES menus(id),
    display_name         VARCHAR2(255) NOT NULL,
    description          CLOB,
    action               VARCHAR2(50) NOT NULL
);

CREATE INDEX idx_permissions_feature_id ON permissions(feature_id);

-- ── APP ROLES ────────────────────────────────────────────────
CREATE TABLE app_roles (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    name                 VARCHAR2(100) NOT NULL,
    display_name         VARCHAR2(255) NOT NULL,
    description          CLOB,
    is_system_role       NUMBER(1) DEFAULT 0 NOT NULL,
    created_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    updated_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_app_roles_tenant_id ON app_roles(tenant_id);
CREATE INDEX idx_app_roles_is_system ON app_roles(is_system_role);

-- ── ROLE-PERMISSION MAPPING ──────────────────────────────────
CREATE TABLE role_permissions (
    role_id              NUMBER(19) NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    permission_id        VARCHAR2(100) NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- ── APP USERS (application users) ───────────────────────────
CREATE TABLE app_users (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    username             VARCHAR2(100) NOT NULL,
    password             VARCHAR2(255) NOT NULL,
    full_name            VARCHAR2(255) NOT NULL,
    email                VARCHAR2(255),
    is_enabled           NUMBER(1) DEFAULT 1 NOT NULL,
    created_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    updated_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    UNIQUE (tenant_id, username)
);

CREATE INDEX idx_app_users_tenant_id ON app_users(tenant_id);
CREATE INDEX idx_app_users_username ON app_users(username);
CREATE INDEX idx_app_users_email ON app_users(email);

-- ── USER-ROLE MAPPING ───────────────────────────────────────
CREATE TABLE user_roles (
    user_id              NUMBER(19) NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id              NUMBER(19) NOT NULL REFERENCES app_roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- ── COMPANY SETTINGS ─────────────────────────────────────────
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
    created_at           TIMESTAMP DEFAULT SYSDATE NOT NULL,
    updated_at           TIMESTAMP DEFAULT SYSDATE NOT NULL
);

CREATE INDEX idx_company_settings_tenant_id ON company_settings(tenant_id);

-- ── AUDIT LOG ────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id                   NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    tenant_id            RAW(16) NOT NULL REFERENCES tenants(id),
    user_id              NUMBER(19) REFERENCES app_users(id),
    action               VARCHAR2(100) NOT NULL,
    entity_type          VARCHAR2(100) NOT NULL,
    entity_id            VARCHAR2(100),
    old_values           CLOB,
    new_values           CLOB,
    timestamp            TIMESTAMP DEFAULT SYSDATE NOT NULL,
    ip_address           VARCHAR2(45),
    user_agent           VARCHAR2(500)
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

