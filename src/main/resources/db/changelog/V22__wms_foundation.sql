--liquibase formatted sql
-- ============================================================
--  V22: Warehouse Management System (WMS) Foundation
-- ============================================================

-- changeset system:V22-001 runOnChange:false

-- ── WAREHOUSES ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS warehouses (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    code             VARCHAR(100) NOT NULL,
    name             VARCHAR(200) NOT NULL,
    address          TEXT,
    manager_name     VARCHAR(200),
    manager_phone    VARCHAR(30),
    manager_email    VARCHAR(200),
    costing_method   VARCHAR(30) NOT NULL DEFAULT 'WEIGHTED_AVERAGE',
    is_default       BOOLEAN NOT NULL DEFAULT FALSE,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_warehouse_code UNIQUE(tenant_id, code)
);

-- ── WAREHOUSE LOCATIONS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS warehouse_locations (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    warehouse_id     BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    barcode          VARCHAR(50) NOT NULL,
    zone             VARCHAR(50),
    aisle            VARCHAR(50),
    rack             VARCHAR(50),
    shelf            VARCHAR(50),
    bin              VARCHAR(50),
    full_address     VARCHAR(200),
    max_capacity_units INT,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_location_barcode UNIQUE(tenant_id, warehouse_id, barcode)
);

-- ── BATCH LOTS ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS batch_lots (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    inventory_item_id    BIGINT NOT NULL REFERENCES inventory_items(id),
    batch_number         VARCHAR(100) NOT NULL,
    serial_number        VARCHAR(100),
    lot_number           VARCHAR(100),
    manufacture_date     DATE,
    expiry_date          DATE,
    received_date        DATE,
    quantity_received    NUMERIC(12,3) NOT NULL DEFAULT 0,
    quantity_available   NUMERIC(12,3) NOT NULL DEFAULT 0,
    unit_cost            NUMERIC(12,4) NOT NULL DEFAULT 0,
    supplier_batch_ref   VARCHAR(100),
    grn_id               BIGINT REFERENCES goods_receipt_notes(id),
    qr_code_data         VARCHAR(500),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_batch_item UNIQUE(tenant_id, inventory_item_id, batch_number)
);

-- ── STOCK BALANCES (real-time per item x location) ────────────────────────────
CREATE TABLE IF NOT EXISTS stock_balances (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    inventory_item_id    BIGINT NOT NULL REFERENCES inventory_items(id),
    location_id          BIGINT NOT NULL REFERENCES warehouse_locations(id),
    quantity_on_hand     NUMERIC(14,3) NOT NULL DEFAULT 0,
    quantity_reserved    NUMERIC(14,3) NOT NULL DEFAULT 0,
    quantity_available   NUMERIC(14,3) NOT NULL DEFAULT 0,
    avg_cost             NUMERIC(14,4) NOT NULL DEFAULT 0,
    reorder_point        NUMERIC(12,3) NOT NULL DEFAULT 0,
    reorder_qty          NUMERIC(12,3) NOT NULL DEFAULT 0,
    alert_sent           BOOLEAN NOT NULL DEFAULT FALSE,
    last_movement_at     TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stock_balance UNIQUE(tenant_id, inventory_item_id, location_id)
);

-- ── STOCK LEDGER (immutable audit log of all movements) ───────────────────────
CREATE TABLE IF NOT EXISTS stock_ledger (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    inventory_item_id    BIGINT NOT NULL REFERENCES inventory_items(id),
    location_id          BIGINT NOT NULL REFERENCES warehouse_locations(id),
    movement_type        VARCHAR(30) NOT NULL,
    transaction_number   VARCHAR(50) NOT NULL,
    quantity             NUMERIC(14,3) NOT NULL,
    unit_cost            NUMERIC(14,4) NOT NULL DEFAULT 0,
    total_cost           NUMERIC(16,4) NOT NULL DEFAULT 0,
    balance_qty          NUMERIC(14,3) NOT NULL DEFAULT 0,
    movement_date        DATE NOT NULL,
    reference_type       VARCHAR(50),
    reference_id         BIGINT,
    reference_number     VARCHAR(100),
    batch_lot_id         BIGINT REFERENCES batch_lots(id),
    remarks              VARCHAR(500),
    created_by           VARCHAR(200),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── STOCK TRANSFERS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_transfers (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    transfer_number      VARCHAR(50) NOT NULL UNIQUE,
    from_location_id     BIGINT NOT NULL REFERENCES warehouse_locations(id),
    to_location_id       BIGINT NOT NULL REFERENCES warehouse_locations(id),
    status               VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    transfer_date        DATE,
    expected_arrival_date DATE,
    received_date        DATE,
    remarks              VARCHAR(500),
    created_by           VARCHAR(200),
    received_by          VARCHAR(200),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── STOCK TRANSFER ITEMS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_transfer_items (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    stock_transfer_id    BIGINT NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    inventory_item_id    BIGINT NOT NULL REFERENCES inventory_items(id),
    requested_qty        NUMERIC(12,3) NOT NULL,
    transferred_qty      NUMERIC(12,3) NOT NULL DEFAULT 0,
    received_qty         NUMERIC(12,3) NOT NULL DEFAULT 0,
    unit                 VARCHAR(20),
    batch_lot_id         BIGINT REFERENCES batch_lots(id),
    remarks              VARCHAR(300)
);

-- ── STOCK COUNTS (physical count sessions) ───────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_counts (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    count_number         VARCHAR(50) NOT NULL UNIQUE,
    warehouse_id         BIGINT NOT NULL REFERENCES warehouses(id),
    location_id          BIGINT REFERENCES warehouse_locations(id),
    status               VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    count_date           DATE NOT NULL,
    completed_at         TIMESTAMPTZ,
    adjustment_posted    BOOLEAN NOT NULL DEFAULT FALSE,
    remarks              VARCHAR(500),
    conducted_by         VARCHAR(200),
    approved_by          VARCHAR(200),
    approved_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── STOCK COUNT ITEMS ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_count_items (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    stock_count_id       BIGINT NOT NULL REFERENCES stock_counts(id) ON DELETE CASCADE,
    inventory_item_id    BIGINT NOT NULL REFERENCES inventory_items(id),
    location_id          BIGINT NOT NULL REFERENCES warehouse_locations(id),
    system_qty           NUMERIC(12,3) NOT NULL DEFAULT 0,
    counted_qty          NUMERIC(12,3) NOT NULL DEFAULT 0,
    variance_qty         NUMERIC(12,3) NOT NULL DEFAULT 0,
    unit                 VARCHAR(20),
    remarks              VARCHAR(300),
    is_scanned           BOOLEAN NOT NULL DEFAULT FALSE
);

-- ── INDEXES ───────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_warehouses_tenant       ON warehouses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wh_locations_tenant     ON warehouse_locations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wh_locations_warehouse  ON warehouse_locations(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_batch_lots_tenant       ON batch_lots(tenant_id);
CREATE INDEX IF NOT EXISTS idx_batch_lots_item         ON batch_lots(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_stock_balances_tenant   ON stock_balances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_balances_item     ON stock_balances(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_stock_balances_location ON stock_balances(location_id);
CREATE INDEX IF NOT EXISTS idx_stock_ledger_tenant     ON stock_ledger(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_ledger_item       ON stock_ledger(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_stock_ledger_location   ON stock_ledger(location_id);
CREATE INDEX IF NOT EXISTS idx_stock_ledger_date       ON stock_ledger(movement_date);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_tenant  ON stock_transfers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_trf_items_tenant  ON stock_transfer_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_counts_tenant     ON stock_counts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_count_items_tenant ON stock_count_items(tenant_id);
