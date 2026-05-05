-- ============================================================
--  V1: Multi-Tenant Foundation Schema
--  Author: Super ERP System
-- ============================================================

-- changeset system:V1-001 runOnChange:false

-- ── Enable required extensions ──────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── SYSTEM USERS (cross-tenant superusers) ──────────────────
CREATE TABLE IF NOT EXISTS system_users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    is_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── TENANTS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(63) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    logo_url        TEXT,
    primary_color   VARCHAR(20) NOT NULL DEFAULT '#3b82f6',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    plan            VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    max_users       INT NOT NULL DEFAULT 10,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    created_by      BIGINT REFERENCES system_users(id)
);

-- ── TENANT SETTINGS (JSONB key-value) ───────────────────────
CREATE TABLE IF NOT EXISTS tenant_settings (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    key         VARCHAR(255) NOT NULL,
    value       JSONB,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, key)
);

-- ── FEATURES (top-level modules) ───────────────────────────
CREATE TABLE IF NOT EXISTS features (
    id              VARCHAR(60) PRIMARY KEY,
    display_name    VARCHAR(255) NOT NULL,
    description     TEXT,
    icon            VARCHAR(100),
    sort_order      INT NOT NULL DEFAULT 0
);

-- ── MENUS (pages within features) ──────────────────────────
CREATE TABLE IF NOT EXISTS menus (
    id              VARCHAR(60) PRIMARY KEY,
    feature_id      VARCHAR(60) NOT NULL REFERENCES features(id),
    display_name    VARCHAR(255) NOT NULL,
    url_pattern     TEXT,
    icon            VARCHAR(100),
    sort_order      INT NOT NULL DEFAULT 0
);

-- ── PERMISSIONS (granular actions) ─────────────────────────
CREATE TABLE IF NOT EXISTS permissions (
    id              VARCHAR(100) PRIMARY KEY,
    feature_id      VARCHAR(60) NOT NULL REFERENCES features(id),
    menu_id         VARCHAR(60) REFERENCES menus(id),
    display_name    VARCHAR(255) NOT NULL,
    description     TEXT,
    action          VARCHAR(50) NOT NULL
);

-- ── TENANT FEATURE MAPPING ──────────────────────────────────
CREATE TABLE IF NOT EXISTS tenant_feature_mapping (
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    feature_id      VARCHAR(60) NOT NULL REFERENCES features(id),
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (tenant_id, feature_id)
);

-- ── APP USERS (tenant-scoped) ───────────────────────────────
CREATE TABLE IF NOT EXISTS app_users (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    username        VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, username)
);

-- ── ROLES (tenant-scoped) ───────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

-- ── ROLE → PERMISSION MAPPING ───────────────────────────────
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   VARCHAR(100) NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- ── USER → ROLE MAPPING ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id         BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── TOKEN BLACKLIST ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS token_blacklist (
    jti             VARCHAR(36) PRIMARY KEY,
    tenant_id       UUID,
    user_id         BIGINT,
    reason          VARCHAR(255),
    blacklisted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL
);

-- ── AUDIT LOG ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID,
    user_id         BIGINT,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100),
    action          VARCHAR(50) NOT NULL,
    old_state_hash  VARCHAR(64),
    new_state_json  JSONB,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── INDEXES ─────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_app_users_tenant      ON app_users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_tenant          ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_token_bl_expires      ON token_blacklist(expires_at);
CREATE INDEX IF NOT EXISTS idx_audit_tenant_time     ON audit_log(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tenant_settings_key   ON tenant_settings(tenant_id, key);
