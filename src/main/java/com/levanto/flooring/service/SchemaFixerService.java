package com.levanto.flooring.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Utility service to fix database schema issues caused by Hibernate's ddl-auto=update.
 * Specifically drops stale CHECK constraints on enum columns so new enum values can be inserted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaFixerService {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixEnumConstraints() {
        try {
            // Find all CHECK constraints on EXPENSES table (Hibernate enum checks)
            List<String> expenseConstraints = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE UPPER(TABLE_NAME) = 'EXPENSES' AND CONSTRAINT_TYPE = 'CHECK'",
                String.class
            );
            
            for (String constraint : expenseConstraints) {
                log.info("Dropping check constraint {} on EXPENSES", constraint);
                jdbcTemplate.execute("ALTER TABLE EXPENSES DROP CONSTRAINT \"" + constraint + "\"");
            }

            // Find all CHECK constraints on PROJECT_EXPENSES table
            List<String> projExpenseConstraints = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE UPPER(TABLE_NAME) = 'PROJECT_EXPENSES' AND CONSTRAINT_TYPE = 'CHECK'",
                String.class
            );

            for (String constraint : projExpenseConstraints) {
                log.info("Dropping check constraint {} on PROJECT_EXPENSES", constraint);
                jdbcTemplate.execute("ALTER TABLE PROJECT_EXPENSES DROP CONSTRAINT \"" + constraint + "\"");
            }
        } catch (Exception e) {
            log.warn("Could not automatically fix schema constraints: {}", e.getMessage());
        }
    }
}
