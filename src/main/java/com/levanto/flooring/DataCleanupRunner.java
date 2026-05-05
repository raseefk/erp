package com.levanto.flooring;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataCleanupRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DataCleanupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Delete the corrupted attendance records or update them to a safe value
            // Since clock_in_time might be the issue, let's just clear the table to be
            // safe,
            // as this is a newly added feature and there shouldn't be much data.
            // jdbcTemplate.execute("DELETE FROM attendance_ledger");
            // System.out.println("DataCleanupRunner: Cleared corrupted attendance
            // records.");
        } catch (Exception e) {
            System.err.println("DataCleanupRunner error: " + e.getMessage());
        }
    }
}
