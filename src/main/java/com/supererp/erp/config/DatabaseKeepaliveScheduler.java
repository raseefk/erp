package com.supererp.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Scheduled job to keep database connections alive.
 * <p>
 * Uses raw JDBC {@link DataSource} directly — intentionally bypasses JPA/Hibernate
 * and Spring transaction management to avoid noisy stack traces when the DB is
 * temporarily unreachable. Failures are logged as a single WARN line only.
 * </p>
 */
@Component
public class DatabaseKeepaliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(DatabaseKeepaliveScheduler.class);

    private final DataSource dataSource;

    public DatabaseKeepaliveScheduler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Fires every 3 minutes (180,000 ms).
     * Adjust via {@code app.db.keepalive.interval-ms} in application.properties.
     */
    @Scheduled(fixedRateString = "${app.db.keepalive.interval-ms:180000}")
    public void keepalive() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            log.debug("DB keepalive ping successful");
        } catch (SQLException e) {
            // Single-line warn — no stack trace spam when DB is temporarily down
            log.warn("DB keepalive ping failed (DB may be restarting): {}", e.getMessage());
        }
    }
}
