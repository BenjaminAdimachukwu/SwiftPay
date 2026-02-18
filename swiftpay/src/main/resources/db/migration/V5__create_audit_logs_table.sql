-- Create audit logs table for compliance and security tracking
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(36),
    username VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    description VARCHAR(500),
    severity VARCHAR(20) DEFAULT 'INFO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_audit_log_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_log_action ON audit_logs(action);
CREATE INDEX idx_audit_log_user ON audit_logs(user_id);
CREATE INDEX idx_audit_log_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_log_ip ON audit_logs(ip_address);

COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for compliance';