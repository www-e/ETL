package com.etl.etl_pipeline.writer;

import com.etl.etl_pipeline.model.ProcessedData;
import com.etl.etl_pipeline.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    
    // Retry configuration - read from application properties
    @Value("${spring.batch.retry.limit:5}")
    private int maxRetries;
    
    @Value("${spring.batch.retry.backoff.initial-interval:1000}")
    private long initialRetryDelayMs;
    
    @Value("${spring.batch.retry.backoff.multiplier:1.5}")
    private double backoffMultiplier;
    
    @Value("${spring.batch.retry.backoff.max-interval:10000}")
    private long maxRetryDelayMs;

    private static final String INSERT_SQL = 
        "INSERT OR REPLACE INTO processed_data (" +
        "id, first_name, last_name, email, birth_date, address, city, country, " +
        "phone_number, salary, dependents, age, tax_rate, net_salary, full_name, " +
        "dependent_allowance, total_deductions, processed_at, processing_status, validation_messages" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void write(@org.springframework.lang.NonNull Chunk<? extends ProcessedData> items) throws Exception {
        if (items.isEmpty()) {
            return;
        }
        
        log.info("Writing {} items to database", items.size());
        
        // Acquire lock to prevent concurrent writes
        dbLock.lock();
        try {
            // Try batch insert first for better performance
            if (items.size() > 1) {
                try {
                    writeBatchWithRetry(items);
                    return;
                } catch (Exception e) {
                    log.warn("Batch insert failed, falling back to individual inserts", e);
                    // Fall back to individual inserts if batch fails
                }
            }
            
            // Individual inserts as fallback
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
        
        while (!success && attempts < maxRetries) {
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
                
                log.debug("Successfully wrote data with ID: {}", data.getId());
                success = true;
            } catch (UncategorizedSQLException e) {
                // Check if it's a database locked error
                if (e.getMessage() != null && e.getMessage().contains("database is locked")) {
                    log.warn("Database locked when writing ID: {}. Attempt {}/{}", data.getId(), attempts, maxRetries);
                    lastException = e;
                    
                    // Wait before retrying with exponential backoff
                    try {
                        long delay = calculateBackoffDelay(attempts);
                        log.debug("Waiting {}ms before retry attempt {}", delay, attempts + 1);
                        Thread.sleep(delay);
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
                    long delay = calculateBackoffDelay(attempts);
                    log.debug("Waiting {}ms before retry attempt {}", delay, attempts + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry delay", ie);
                }
            }
        }
        
        if (!success) {
            log.error("Failed to write data with ID: {} after {} attempts", data.getId(), maxRetries);
            if (lastException != null) {
                throw lastException;
            } else {
                throw new RuntimeException("Failed to write data with ID: " + data.getId() + " after " + maxRetries + " attempts");
            }
        }
    }
    
    /**
     * Write a batch of data with retry logic
     * 
     * @param items The data items to write
     * @throws Exception If writing fails after all retries
     */
    private void writeBatchWithRetry(Chunk<? extends ProcessedData> items) throws Exception {
        int attempts = 0;
        boolean success = false;
        Exception lastException = null;
        
        while (!success && attempts < maxRetries) {
            attempts++;
            try {
                final Chunk<? extends ProcessedData> itemsRef = items;
                
                jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ProcessedData data = itemsRef.getItems().get(i);
                        ps.setString(1, data.getId());
                        ps.setString(2, data.getFirstName());
                        ps.setString(3, data.getLastName());
                        ps.setString(4, data.getEmail());
                        ps.setString(5, data.getBirthDate() != null ? DateUtils.formatDate(data.getBirthDate()) : null);
                        ps.setString(6, data.getAddress());
                        ps.setString(7, data.getCity());
                        ps.setString(8, data.getCountry());
                        ps.setString(9, data.getPhoneNumber());
                        ps.setDouble(10, data.getSalary() != null ? data.getSalary() : 0);
                        ps.setInt(11, data.getDependents() != null ? data.getDependents() : 0);
                        ps.setInt(12, data.getAge() != null ? data.getAge() : 0);
                        ps.setDouble(13, data.getTaxRate() != null ? data.getTaxRate() : 0);
                        ps.setDouble(14, data.getNetSalary() != null ? data.getNetSalary() : 0);
                        ps.setString(15, data.getFullName());
                        ps.setDouble(16, data.getDependentAllowance() != null ? data.getDependentAllowance() : 0);
                        ps.setDouble(17, data.getTotalDeductions() != null ? data.getTotalDeductions() : 0);
                        ps.setString(18, data.getProcessedAt() != null ? data.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                        ps.setString(19, data.getProcessingStatus());
                        ps.setString(20, data.getValidationMessages());
                    }
                    
                    @Override
                    public int getBatchSize() {
                        return itemsRef.size();
                    }
                });
                
                log.info("Successfully wrote batch of {} items", items.size());
                success = true;
            } catch (UncategorizedSQLException e) {
                // Check if it's a database locked error
                if (e.getMessage() != null && e.getMessage().contains("database is locked")) {
                    log.warn("Database locked when writing batch. Attempt {}/{}", attempts, maxRetries);
                    lastException = e;
                    
                    // Wait before retrying with exponential backoff
                    try {
                        long delay = calculateBackoffDelay(attempts);
                        log.debug("Waiting {}ms before retry attempt {}", delay, attempts + 1);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted during retry delay", ie);
                    }
                } else {
                    // Not a locking issue, rethrow
                    log.error("SQL error writing batch", e);
                    throw e;
                }
            } catch (DataAccessException e) {
                log.error("Database error writing batch", e);
                lastException = e;
                
                // Wait before retrying
                try {
                    long delay = calculateBackoffDelay(attempts);
                    log.debug("Waiting {}ms before retry attempt {}", delay, attempts + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry delay", ie);
                }
            }
        }
        
        if (!success) {
            log.error("Failed to write batch after {} attempts", maxRetries);
            if (lastException != null) {
                throw lastException;
            } else {
                throw new RuntimeException("Failed to write batch after " + maxRetries + " attempts");
            }
        }
    }
    
    /**
     * Calculate backoff delay with exponential increase
     * 
     * @param attempt The current attempt number (1-based)
     * @return The delay in milliseconds
     */
    private long calculateBackoffDelay(int attempt) {
        // Calculate exponential backoff with jitter
        double exponentialFactor = Math.pow(backoffMultiplier, attempt - 1);
        long delay = (long) (initialRetryDelayMs * exponentialFactor);
        
        // Add some randomness (jitter) to prevent synchronized retries
        delay += (long) (delay * 0.2 * Math.random());
        
        // Cap at max delay
        return Math.min(delay, maxRetryDelayMs);
    }
}
