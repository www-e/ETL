package com.etl.etl_pipeline.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic controller for debugging database issues
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get database schema information
     * @return Map with schema information
     */
    @GetMapping("/schema")
    public ResponseEntity<Map<String, Object>> getDatabaseSchema() {
        log.info("Getting database schema information");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get table structure
            List<Map<String, Object>> tableInfo = jdbcTemplate.queryForList(
                "PRAGMA table_info(processed_data)"
            );
            
            // Get a sample row to check data
            List<Map<String, Object>> sampleData = jdbcTemplate.queryForList(
                "SELECT * FROM processed_data LIMIT 1"
            );
            
            // Get count of rows
            Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_data", 
                Integer.class
            );
            
            response.put("status", "success");
            response.put("tableColumns", tableInfo);
            response.put("sampleData", sampleData);
            response.put("rowCount", rowCount != null ? rowCount : 0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting database schema", e);
            response.put("status", "error");
            response.put("message", "Failed to get database schema: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Reset the database by recreating the processed_data table
     * @return Map with operation status
     */
    @GetMapping("/reset-schema")
    public ResponseEntity<Map<String, Object>> resetSchema() {
        log.info("Resetting database schema");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a backup of existing data
            jdbcTemplate.execute("BEGIN TRANSACTION");
            
            // Create a new table with all required columns
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
            
            // Copy data from the old table to the new one if it exists
            try {
                jdbcTemplate.execute(
                    "INSERT INTO processed_data_new " +
                    "SELECT id, first_name, last_name, email, birth_date, address, city, country, " +
                    "phone_number, salary, dependents, age, tax_rate, net_salary, full_name, " +
                    "dependent_allowance, total_deductions, " +
                    "COALESCE(bonus, 0) as bonus, " +
                    "COALESCE(retirement_contribution, 0) as retirement_contribution, " +
                    "COALESCE(total_compensation, 0) as total_compensation, " +
                    "COALESCE(tax_amount, 0) as tax_amount, " +
                    "processed_at, processing_status, validation_messages " +
                    "FROM processed_data"
                );
            } catch (Exception e) {
                log.warn("Could not copy data from old table: {}", e.getMessage());
                // Continue with the schema reset even if copying fails
            }
            
            // Drop the old table and rename the new one
            jdbcTemplate.execute("DROP TABLE IF EXISTS processed_data");
            jdbcTemplate.execute("ALTER TABLE processed_data_new RENAME TO processed_data");
            
            jdbcTemplate.execute("COMMIT");
            
            response.put("status", "success");
            response.put("message", "Database schema reset successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            jdbcTemplate.execute("ROLLBACK");
            log.error("Error resetting database schema", e);
            response.put("status", "error");
            response.put("message", "Failed to reset database schema: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
