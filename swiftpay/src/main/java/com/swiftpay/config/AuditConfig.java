package com.swiftpay.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Audit configuration for automatic tracking of entity changes.
 *
 * Automatically populates:
 * - createdBy: Username of user who created the entity
 * - createdAt: Timestamp when entity was created
 * - updatedBy: Username of user who last modified the entity
 * - updatedAt: Timestamp when entity was last modified
 *
 * @author SwiftPay Engineering Team
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {
    /**
     * Provides the current auditor (user) for JPA auditing.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * Implementation of AuditorAware that retrieves the current user
     * from Spring Security context.
     */
    public static class AuditorAwareImpl implements AuditorAware<String> {

        /**
         * Get the current auditor (user).
         *
         * Returns:
         * 1. Authenticated user from Security Context
         * 2. "SYSTEM" if no authentication exists
         * 3. "ANONYMOUS" for anonymous users
         */
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }

            if (authentication.getPrincipal() instanceof String
                    && "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("ANONYMOUS");
            }

            String username = authentication.getName();

            if (username == null || username.trim().isEmpty()) {
                return Optional.of("SYSTEM");
            }

            return Optional.of(username);
        }
    }
}
