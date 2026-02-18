-- Create transactions table for payment processing
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_reference VARCHAR(50) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    payment_method VARCHAR(30) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    processing_fee NUMERIC(19,4) DEFAULT 0.0000,
    net_amount NUMERIC(19,4),
    source_account_id UUID,
    destination_account_id UUID,
    description VARCHAR(500),
    metadata TEXT,
    gateway_reference VARCHAR(100),
    gateway_response_code VARCHAR(50),
    gateway_response_message VARCHAR(500),
    parent_transaction_id UUID,
    client_ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    is_test_transaction BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_transaction_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transaction_destination_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transaction_parent FOREIGN KEY (parent_transaction_id) REFERENCES transactions(id)
);

CREATE UNIQUE INDEX idx_transaction_reference ON transactions(transaction_reference);
CREATE UNIQUE INDEX idx_transaction_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transaction_source_account ON transactions(source_account_id);
CREATE INDEX idx_transaction_destination_account ON transactions(destination_account_id);
CREATE INDEX idx_transaction_status ON transactions(status);
CREATE INDEX idx_transaction_type ON transactions(transaction_type);
CREATE INDEX idx_transaction_created_at ON transactions(created_at);
CREATE INDEX idx_transaction_completed_at ON transactions(completed_at);
CREATE INDEX idx_transaction_parent ON transactions(parent_transaction_id);

ALTER TABLE transactions ADD CONSTRAINT chk_transaction_amount CHECK (amount > 0);

COMMENT ON TABLE transactions IS 'All financial transactions in the system';