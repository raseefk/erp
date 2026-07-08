package com.supererp.erp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Ensures critical environment variables are set when running in the 'prod' profile.
 */
@Component
@Slf4j
public class StartupValidator implements CommandLineRunner {

    private final Environment env;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public StartupValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(String... args) {
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains("prod")) {
            log.info("StartupValidator: Verifying production configuration...");
            validateSecret("app.jwt.secret",            jwtSecret);
            validateSecret("spring.datasource.password", dbPassword);
            validateSecret("app.admin.password",         adminPassword);
            log.info("StartupValidator: Production configuration OK.");
        }
    }

    private void validateSecret(String name, String value) {
        if (value == null || value.trim().isEmpty() || value.contains("${")) {
            log.error("CRITICAL: Production secret '{}' is not set.", name);
            throw new IllegalStateException("Missing required production secret: " + name);
        }
        if (value.equalsIgnoreCase("secret") || value.equalsIgnoreCase("password")) {
            log.warn("WARNING: Production secret '{}' appears to be a placeholder value.", name);
        }
    }
}
