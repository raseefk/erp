-- V6: Add tagline column to company_settings table
ALTER TABLE company_settings ADD COLUMN IF NOT EXISTS tagline VARCHAR(200);

-- Update existing records with a default tagline
UPDATE company_settings SET tagline = 'Enterprise ERP System' WHERE tagline IS NULL;
