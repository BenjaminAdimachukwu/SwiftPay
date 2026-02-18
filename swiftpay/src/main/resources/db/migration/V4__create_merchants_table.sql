-- Create merchants table for business accounts
CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_code VARCHAR(20) NOT NULL UNIQUE,
    business_name VARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(2),
    api_key_hash VARCHAR(255) NOT NULL UNIQUE,
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),
    commission_rate NUMERIC(5,2) DEFAULT 2.50,
    daily_transaction_limit NUMERIC(19,4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    tax_id VARCHAR(50),
    account_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_merchant_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE UNIQUE INDEX idx_merchant_code ON merchants(merchant_code);
CREATE UNIQUE INDEX idx_merchant_api_key ON merchants(api_key_hash);
CREATE INDEX idx_merchant_status ON merchants(is_active, is_verified);

COMMENT ON TABLE merchants IS 'Merchant/business accounts that accept payments';