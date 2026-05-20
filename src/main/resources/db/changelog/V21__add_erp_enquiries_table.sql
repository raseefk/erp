-- V21: Create erp_enquiries table for general ERP landing page enquiries
CREATE TABLE IF NOT EXISTS erp_enquiries (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(30) NOT NULL,
    company         VARCHAR(255),
    industry        VARCHAR(100),
    plan            VARCHAR(50),
    message         TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'NEW',
    admin_notes     TEXT,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
