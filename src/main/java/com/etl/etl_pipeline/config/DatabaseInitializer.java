package com.etl.etl_pipeline.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database initializer to ensure the schema is up to date with all required columns
 */
@Slf4j
@Configuration
public class DatabaseInitializer {

    // No longer need DataSource as we're using JdbcTemplate
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Initialize the database with schema updates
     */
    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing database schema");
            
            // First, check if the processed_data table exists
            boolean tableExists = tableExists("processed_data");
            
            if (!tableExists) {
                // Create the table with all required columns
                createProcessedDataTable();
                log.info("Created processed_data table with all required columns");
                return;
            }
            
            // Get existing columns
            Set<String> existingColumns = getExistingColumns("processed_data");
            log.info("Existing columns in processed_data table: {}", existingColumns);
            
            // Check if we need to recreate the table
            boolean needsRecreation = false;
            List<String> missingColumns = new ArrayList<>();
            
            // Define required columns
            String[] requiredColumns = {"bonus", "retirement_contribution", "total_compensation", "tax_amount"};
            
            // Check for missing columns
            for (String column : requiredColumns) {
                if (!existingColumns.contains(column.toLowerCase())) {
                    missingColumns.add(column);
                    needsRecreation = true;
                }
            }
            
            if (needsRecreation) {
                log.info("Missing columns detected: {}. Recreating table with all required columns.", missingColumns);
                recreateProcessedDataTable();
            } else {
                log.info("All required columns exist in the processed_data table");
            }
            
            log.info("Database schema initialization completed successfully");
        } catch (Exception e) {
            log.error("Error initializing database schema", e);
        }
    }
    
    /**
     * Check if a table exists in the database
     */
    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?", 
                Integer.class, 
                tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Error checking if table exists: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get existing columns for a table using PRAGMA table_info
     */
    private Set<String> getExistingColumns(String tableName) {
        Set<String> columns = new HashSet<>();
        try {
            jdbcTemplate.query(
                "PRAGMA table_info(" + tableName + ")",
                (java.sql.ResultSet rs) -> {
                    while (rs.next()) {
                        columns.add(rs.getString("name").toLowerCase());
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            log.error("Error getting existing columns: {}", e.getMessage());
        }
        return columns;
    }
    
    /**
     * Create the processed_data table with all required columns
     */
    private void createProcessedDataTable() {
        String createTableSQL = 
            "CREATE TABLE processed_data (" +
            "id TEXT PRIMARY KEY, " +
            "first_name TEXT, " +
            "last_name TEXT, " +
            "email TEXT, " +
            "birth_date TEXT, " +
            "address TEXT, " +
            "city TEXT, " +
            "country TEXT, " +
            "phone_number TEXT, " +
            "salary DOUBLE, " +
            "dependents INTEGER, " +
            "age INTEGER, " +
            "tax_rate DOUBLE, " +
            "net_salary DOUBLE, " +
            "full_name TEXT, " +
            "dependent_allowance DOUBLE, " +
            "total_deductions DOUBLE, " +
            "bonus DOUBLE, " +
            "retirement_contribution DOUBLE, " +
            "total_compensation DOUBLE, " +
            "tax_amount DOUBLE, " +
            "processed_at TEXT, " +
            "processing_status TEXT, " +
            "validation_messages TEXT" +
            ")"; 
        
        jdbcTemplate.execute(createTableSQL);
    }
    
    /**
     * Recreate the processed_data table with all required columns
     * This is a more reliable approach for SQLite which has limitations with ALTER TABLE
     */
    private void recreateProcessedDataTable() {
        try {
            // Create a backup table
            jdbcTemplate.execute("BEGIN TRANSACTION");
            
            // Create a new table with the correct schema
            jdbcTemplate.execute("DROP TABLE IF EXISTS processed_data_new");
            
            jdbcTemplate.execute(
                "CREATE TABLE processed_data_new (" +
                "id TEXT PRIMARY KEY, " +
                "first_name TEXT, " +
                "last_name TEXT, " +
                "email TEXT, " +
                "birth_date TEXT, " +
                "address TEXT, " +
                "city TEXT, " +
                "country TEXT, " +
                "phone_number TEXT, " +
                "salary DOUBLE, " +
                "dependents INTEGER, " +
                "age INTEGER, " +
                "tax_rate DOUBLE, " +
                "net_salary DOUBLE, " +
                "full_name TEXT, " +
                "dependent_allowance DOUBLE, " +
                "total_deductions DOUBLE, " +
                "bonus DOUBLE, " +
                "retirement_contribution DOUBLE, " +
                "total_compensation DOUBLE, " +
                "tax_amount DOUBLE, " +
                "processed_at TEXT, " +
                "processing_status TEXT, " +
                "validation_messages TEXT" +
                ")"
            );
            
            // Get existing columns from the old table
            Set<String> existingColumns = getExistingColumns("processed_data");
            
            // Build a list of columns to copy
            StringBuilder columnList = new StringBuilder();
            for (String column : existingColumns) {
                if (columnList.length() > 0) {
                    columnList.append(", ");
                }
                columnList.append(column);
            }
            
            // Copy data from the old table to the new one
            if (!existingColumns.isEmpty()) {
                jdbcTemplate.execute(
                    "INSERT INTO processed_data_new (" + columnList + ") " +
                    "SELECT " + columnList + " FROM processed_data"
                );
            }
            
            // Drop the old table and rename the new one
            jdbcTemplate.execute("DROP TABLE processed_data");
            jdbcTemplate.execute("ALTER TABLE processed_data_new RENAME TO processed_data");
            
            jdbcTemplate.execute("COMMIT");
            
            log.info("Successfully recreated processed_data table with all required columns");
        } catch (Exception e) {
            jdbcTemplate.execute("ROLLBACK");
            log.error("Error recreating processed_data table: {}", e.getMessage());
        }
    }
}
