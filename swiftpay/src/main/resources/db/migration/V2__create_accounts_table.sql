-- Create accounts table for customer financial accounts
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(34) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    available_balance NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    reserved_balance NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    overdraft_limit NUMERIC(19,4) DEFAULT 0.0000,
    daily_limit NUMERIC(19,4),
    monthly_limit NUMERIC(19,4),
    single_transaction_limit NUMERIC(19,4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_frozen BOOLEAN NOT NULL DEFAULT FALSE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    freeze_reason VARCHAR(255),
    customer_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_account_number ON accounts(account_number);
CREATE INDEX idx_account_customer ON accounts(customer_id);
CREATE INDEX idx_account_status ON accounts(is_active, is_frozen);
CREATE INDEX idx_account_type_currency ON accounts(account_type, currency);
CREATE UNIQUE INDEX uk_customer_type_currency ON accounts(customer_id, account_type, currency);

ALTER TABLE accounts ADD CONSTRAINT chk_account_balance CHECK (balance >= 0);

COMMENT ON TABLE accounts IS 'Financial accounts belonging to customers';