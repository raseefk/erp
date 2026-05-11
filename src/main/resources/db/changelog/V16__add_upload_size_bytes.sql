-- V16: Add upload_size_bytes to tenants
ALTER TABLE tenants ADD COLUMN upload_size_bytes BIGINT DEFAULT 0;

-- Optional: Initial backfill (this may be slow on huge databases, but it's okay for now)
UPDATE tenants SET upload_size_bytes = 0 WHERE upload_size_bytes IS NULL;
