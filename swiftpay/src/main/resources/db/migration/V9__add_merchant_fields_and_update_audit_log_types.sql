-- Add missing columns to merchants table
ALTER TABLE merchants
    ADD COLUMN is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN business_registration_number VARCHAR(50) UNIQUE,
    ADD COLUMN last_settlement_at TIMESTAMP;

-- Update audit_logs columns from VARCHAR to UUID
ALTER TABLE audit_logs
    ALTER COLUMN entity_id TYPE UUID USING entity_id::uuid,
    ALTER COLUMN user_id TYPE UUID USING user_id::uuid;
