-- Additional indexes and optimizations for better query performance

-- Composite indexes for common query patterns
CREATE INDEX idx_transactions_status_created_at ON transactions(status, created_at);
CREATE INDEX idx_transactions_account_status ON transactions(source_account_id, status);
CREATE INDEX idx_accounts_customer_active ON accounts(customer_id, is_active);
CREATE INDEX idx_customers_role_active ON customers(role, is_active);

-- Partial indexes for specific use cases
CREATE INDEX idx_transactions_pending ON transactions(status) WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX idx_transactions_failed ON transactions(status) WHERE status = 'FAILED';
CREATE INDEX idx_accounts_frozen ON accounts(is_frozen) WHERE is_frozen = TRUE;
CREATE INDEX idx_customers_locked ON customers(is_locked) WHERE is_locked = TRUE;

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for automatic updated_at management
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transactions_updated_at BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchants_updated_at BEFORE UPDATE ON merchants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON FUNCTION update_updated_at_column() IS 'Automatically updates the updated_at timestamp';