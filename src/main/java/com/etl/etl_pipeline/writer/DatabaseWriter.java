package com.etl.etl_pipeline.writer;

import com.etl.etl_pipeline.model.ProcessedData;
import com.etl.etl_pipeline.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Writer for saving processed data to SQLite database
 * Includes retry logic and synchronization to handle SQLite database locking issues
 */
@Slf4j
@Component
public class DatabaseWriter implements ItemWriter<ProcessedData> {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // Lock to synchronize database writes
    private static final ReentrantLock dbLock = new ReentrantLock();
    
    // Retry configuration
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 200;

    private static final String INSERT_SQL = 
        "INSERT OR REPLACE INTO processed_data (" +
        "id, first_name, last_name, email, birth_date, address, city, country, " +
        "phone_number, salary, dependents, age, tax_rate, net_salary, full_name, " +
        "dependent_allowance, total_deductions, processed_at, processing_status, validation_messages" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void write(@org.springframework.lang.NonNull Chunk<? extends ProcessedData> items) throws Exception {
        log.info("Writing {} items to database", items.size());
        
        // Acquire lock to prevent concurrent writes
        dbLock.lock();
        try {
            for (ProcessedData data : items) {
                writeWithRetry(data);
            }
        } finally {
            // Always release the lock
            dbLock.unlock();
        }
    }
    
    /**
     * Write data with retry logic for handling SQLite database locking issues
     * 
     * @param data The data to write
     * @throws Exception If writing fails after all retries
     */
    private void writeWithRetry(ProcessedData data) throws Exception {
        int attempts = 0;
        boolean success = false;
        Exception lastException = null;
        
        while (!success && attempts < MAX_RETRIES) {
            attempts++;
            try {
                jdbcTemplate.update(
                    INSERT_SQL,
                    data.getId(),
                    data.getFirstName(),
                    data.getLastName(),
                    data.getEmail(),
                    data.getBirthDate() != null ? DateUtils.formatDate(data.getBirthDate()) : null,
                    data.getAddress(),
                    data.getCity(),
                    data.getCountry(),
                    data.getPhoneNumber(),
                    data.getSalary(),
                    data.getDependents(),
                    data.getAge(),
                    data.getTaxRate(),
                    data.getNetSalary(),
                    data.getFullName(),
                    data.getDependentAllowance(),
                    data.getTotalDeductions(),
                    data.getProcessedAt() != null ? data.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    data.getProcessingStatus(),
                    data.getValidationMessages()
                );
                
                log.info("Successfully wrote data with ID: {}", data.getId());
                success = true;
            } catch (UncategorizedSQLException e) {
                // Check if it's a database locked error
                if (e.getMessage() != null && e.getMessage().contains("database is locked")) {
                    log.warn("Database locked when writing ID: {}. Attempt {}/{}", data.getId(), attempts, MAX_RETRIES);
                    lastException = e;
                    
                    // Wait before retrying
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted during retry delay", ie);
                    }
                } else {
                    // Not a locking issue, rethrow
                    log.error("SQL error writing data with ID: {}", data.getId(), e);
                    throw e;
                }
            } catch (DataAccessException e) {
                log.error("Database error writing data with ID: {}", data.getId(), e);
                lastException = e;
                
                // Wait before retrying
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempts); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry delay", ie);
                }
            }
        }
        
        if (!success) {
            log.error("Failed to write data with ID: {} after {} attempts", data.getId(), MAX_RETRIES);
            if (lastException != null) {
                throw lastException;
            } else {
                throw new RuntimeException("Failed to write data with ID: " + data.getId() + " after " + MAX_RETRIES + " attempts");
            }
        }
    }
}
