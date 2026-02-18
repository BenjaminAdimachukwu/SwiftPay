package com.swiftpay.domain.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

/**
 * AuditLog entity for security and compliance audit trails.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_log_action", columnList = "action"),
                @Index(name = "idx_audit_log_user", columnList = "user_id"),
                @Index(name = "idx_audit_log_created_at", columnList = "created_at"),
                @Index(name = "idx_audit_log_ip", columnList = "ip_address")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "severity", length = 20)
    @Builder.Default
    private String severity = "INFO";

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + getId() +
                ", entityType='" + entityType + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
