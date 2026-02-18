-- Add transaction limit tracking columns
ALTER TABLE accounts
    ADD COLUMN daily_transaction_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE accounts
    ADD COLUMN daily_transaction_limit INTEGER NOT NULL DEFAULT 10;

ALTER TABLE accounts
    ADD COLUMN last_transaction_date TIMESTAMP;

-- Add indexes for better query performance
CREATE INDEX idx_account_is_deleted
    ON accounts(is_deleted);

CREATE INDEX idx_account_transaction_count
    ON accounts(daily_transaction_count)
    WHERE is_deleted = false;

CREATE INDEX idx_account_daily_limit_check
    ON accounts(daily_transaction_count, daily_transaction_limit)
    WHERE is_deleted = false;

-- Add comments for documentation
COMMENT ON COLUMN accounts.is_deleted
IS 'Soft delete flag - true if account is deleted';

COMMENT ON COLUMN accounts.daily_transaction_count
IS 'Current number of transactions performed today';

COMMENT ON COLUMN accounts.daily_transaction_limit
IS 'Maximum allowed transactions per day';

COMMENT ON COLUMN accounts.last_transaction_date
IS 'Timestamp of last transaction for daily reset logic';