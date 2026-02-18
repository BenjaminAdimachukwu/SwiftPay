package com.swiftpay.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    SUPER_ADMIN("Super Admin", "Full system access and control", 100, Set.of("*")),
    ADMIN("Administrator", "System administration access", 90, Set.of(
            "user:create", "user:read", "user:update", "user:delete",
            "transaction:read", "transaction:update", "account:read"
    )),
    COMPLIANCE_OFFICER("Compliance Officer", "Regulatory compliance", 80, Set.of(
            "transaction:read", "transaction:review", "account:read", "audit:view"
    )),
    SUPPORT("Customer Support", "Customer support and assistance", 70, Set.of(
            "customer:read", "customer:update", "transaction:read", "account:read"
    )),
    MERCHANT("Merchant", "Business user receiving payments", 50, Set.of(
            "payment:receive", "payment:refund", "transaction:read", "account:read"
    )),
    CUSTOMER("Customer", "Regular user making payments", 30, Set.of(
            "payment:send", "transaction:read", "account:read", "profile:manage"
    )),
    GUEST("Guest", "Limited access guest user", 10, Set.of("public:view")),
    API_CLIENT("API Client", "Programmatic API access", 60, Set.of(
            "api:access", "payment:create", "payment:read", "transaction:read"
    ));

    private final String displayName;
    private final String description;
    private final int priority;
    private final Set<String> permissions;

    public boolean hasPermission(String permission) {
        if (this == SUPER_ADMIN) {
            return true;
        }
        return permissions.contains(permission) || permissions.contains("*");
    }

    public boolean hasHigherPriorityThan(UserRole other) {
        return this.priority > other.priority;
    }

    public static List<UserRole> getAdministrativeRoles() {
        return Arrays.asList(SUPER_ADMIN, ADMIN, COMPLIANCE_OFFICER);
    }

    public static UserRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }

        String normalizedValue = value.replace("ROLE_", "");

        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(normalizedValue) ||
                    role.displayName.equalsIgnoreCase(normalizedValue)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Invalid user role: " + value);
    }

    public String getSpringSecurityRole() {
        return "ROLE_" + this.name();
    }
}
