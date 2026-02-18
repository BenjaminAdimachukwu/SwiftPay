package com.swiftpay.domain.entity;
import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

/**
 * PaymentGatewayLog entity for tracking all interactions with payment gateways.
 */
@Entity
@Table(
        name = "payment_gateway_logs",
        indexes = {
                @Index(name = "idx_gateway_log_transaction", columnList = "transaction_id"),
                @Index(name = "idx_gateway_log_gateway_ref", columnList = "gateway_reference"),
                @Index(name = "idx_gateway_log_created_at", columnList = "created_at"),
                @Index(name = "idx_gateway_log_status", columnList = "response_status_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", foreignKey = @ForeignKey(name = "fk_gateway_log_transaction"))
    private Transaction transaction;

    @Column(name = "gateway_name", nullable = false, length = 50)
    private String gatewayName;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    @Column(name = "request_url", nullable = false, length = 500)
    private String requestUrl;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "is_success", nullable = false)
    @Builder.Default
    private Boolean isSuccess = false;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Override
    public String toString() {
        return "PaymentGatewayLog{" +
                "id=" + getId() +
                ", gatewayName='" + gatewayName + '\'' +
                ", responseStatusCode=" + responseStatusCode +
                '}';
    }
}
