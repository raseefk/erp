package com.supererp.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Ensures that the application fails fast if critical environment variables
 * are missing when running in the 'prod' profile.
 */
@Component
@Slf4j
public class StartupValidator implements CommandLineRunner {

    private final Environment env;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.system.admin.password:}")
    private String systemAdminPassword;

    public StartupValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(String... args) {
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        
        if (activeProfiles.contains("prod")) {
            log.info("StartupValidator: Verifying production configuration...");

            validateSecret("app.jwt.secret", jwtSecret);
            validateSecret("spring.datasource.password", dbPassword);
            validateSecret("app.system.admin.password", systemAdminPassword);

            log.info("StartupValidator: Production configuration looks valid.");
        }
    }

    private void validateSecret(String name, String value) {
        if (value == null || value.trim().isEmpty() || value.contains("${")) {
            log.error("CRITICAL ERROR: Production secret '{}' is NOT set. Application cannot start in 'prod' profile safely.", name);
            throw new IllegalStateException("Missing required production secret: " + name);
        }
        
        // Basic check for placeholder values that might have been accidentally left
        if (value.equalsIgnoreCase("secret") || value.equalsIgnoreCase("password") || value.contains("placeholder")) {
             log.warn("WARNING: Production secret '{}' seems to have a generic placeholder value. Please ensure this is rotated.", name);
        }
    }
}
