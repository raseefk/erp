-- Add missing HR fields to employees table
ALTER TABLE employees ADD COLUMN dob DATE;
ALTER TABLE employees ADD COLUMN address VARCHAR(500);
ALTER TABLE employees ADD COLUMN aadhaar_number VARCHAR(20);
ALTER TABLE employees ADD COLUMN pan_number VARCHAR(20);
ALTER TABLE employees ADD COLUMN bank_name VARCHAR(100);
ALTER TABLE employees ADD COLUMN account_number VARCHAR(50);
ALTER TABLE employees ADD COLUMN ifsc_code VARCHAR(20);
