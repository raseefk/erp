-- liquibase formatted sql

-- changeset supererp:v4-add-max-storage
ALTER TABLE tenants ADD COLUMN max_storage_gb DOUBLE PRECISION DEFAULT 5.0;
