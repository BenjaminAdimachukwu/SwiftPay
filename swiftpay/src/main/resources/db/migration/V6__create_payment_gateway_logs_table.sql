-- Create payment gateway logs for tracking external API interactions
CREATE TABLE payment_gateway_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID,
    gateway_name VARCHAR(50) NOT NULL,
    gateway_reference VARCHAR(100),
    request_method VARCHAR(10) NOT NULL,
    request_url VARCHAR(500) NOT NULL,
    request_headers TEXT,
    request_body TEXT,
    response_status_code INTEGER,
    response_headers TEXT,
    response_body TEXT,
    response_time_ms BIGINT,
    is_success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_gateway_log_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_gateway_log_transaction ON payment_gateway_logs(transaction_id);
CREATE INDEX idx_gateway_log_gateway_ref ON payment_gateway_logs(gateway_reference);
CREATE INDEX idx_gateway_log_created_at ON payment_gateway_logs(created_at);
CREATE INDEX idx_gateway_log_status ON payment_gateway_logs(response_status_code);

COMMENT ON TABLE payment_gateway_logs IS 'Logs of all payment gateway API interactions';