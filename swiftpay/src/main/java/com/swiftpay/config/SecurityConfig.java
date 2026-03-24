package com.swiftpay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for SwiftPay API.
 *
 * For now (development/testing):
 * - CSRF disabled (not needed for REST APIs)
 * - All endpoints permit all (no authentication required)
 *
 * In Day 5, we'll add:
 * - JWT authentication
 * - Role-based access control
 * - Proper endpoint security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for stateless REST APIs)
                .csrf(csrf -> csrf.disable())

                // Permit all requests (no authentication required for now)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}