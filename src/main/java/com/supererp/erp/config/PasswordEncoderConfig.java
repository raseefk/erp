package com.supererp.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Isolated configuration for password encoding.
 * Kept separate from SecurityConfig to avoid circular dependency cycles
 * (e.g., TenantService → PasswordEncoder → SecurityConfig → TenantResolutionFilter → TenantService).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
