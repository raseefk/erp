package com.supererp.erp.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Drops stale CHECK constraints on enum columns so new enum values can be inserted.
 * Adapted for Oracle — uses USER_CONSTRAINTS instead of INFORMATION_SCHEMA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaFixerService {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixEnumConstraints() {
        dropCheckConstraintsForTable("EXPENSES");
        dropCheckConstraintsForTable("PROJECT_EXPENSES");
    }

    private void dropCheckConstraintsForTable(String tableName) {
        try {
            List<String> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM user_constraints " +
                "WHERE table_name = ? AND constraint_type = 'C' " +
                "AND constraint_name NOT LIKE 'SYS_%'",
                String.class, tableName
            );
            for (String c : constraints) {
                log.info("Dropping check constraint {} on {}", c, tableName);
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT \"" + c + "\"");
            }
        } catch (Exception e) {
            log.warn("Could not fix schema constraints for {}: {}", tableName, e.getMessage());
        }
    }
}
