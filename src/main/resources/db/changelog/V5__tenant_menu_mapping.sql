-- V5: Add tenant_menu_mapping table for granular per-tenant menu control
-- A menu is ENABLED by default; a row only needs to exist when it's disabled.

CREATE TABLE IF NOT EXISTS tenant_menu_mapping (
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    menu_id     VARCHAR(60) NOT NULL REFERENCES menus(id)   ON DELETE CASCADE,
    is_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (tenant_id, menu_id)
);

-- Index for fast lookup by tenant
CREATE INDEX IF NOT EXISTS idx_tenant_menu_mapping_tenant
    ON tenant_menu_mapping (tenant_id);
