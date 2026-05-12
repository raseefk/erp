-- V19: Add job_card_id to subcontractor_running_bills
ALTER TABLE subcontractor_running_bills
    ADD COLUMN IF NOT EXISTS job_card_id BIGINT REFERENCES job_cards(id) ON DELETE SET NULL;
