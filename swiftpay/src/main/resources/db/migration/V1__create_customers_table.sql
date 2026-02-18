
CREATE TABLE customers (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
first_name VARCHAR(50) NOT NULL,
last_name VARCHAR(50) NOT NULL,
email VARCHAR(100) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL,
phone_number VARCHAR(20),
date_of_birth DATE,
address_line1 VARCHAR(255),
address_line2 VARCHAR(255),
city VARCHAR(100),
state VARCHAR(100),
postal_code VARCHAR(20),
country VARCHAR(2),
role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
is_locked BOOLEAN NOT NULL DEFAULT FALSE,
kyc_verified BOOLEAN NOT NULL DEFAULT FALSE,
kyc_verified_at DATE,
kyc_level INTEGER DEFAULT 0,
failed_login_attempts INTEGER NOT NULL DEFAULT 0,
last_login_at DATE,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
version BIGINT NOT NULL DEFAULT 0,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_customer_phone ON customers(phone_number);
CREATE INDEX idx_customer_status ON customers(is_active, is_email_verified);
CREATE INDEX idx_customer_created_at ON customers(created_at);

COMMENT ON TABLE customers IS 'Stores customer/user information';

