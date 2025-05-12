package com.etl.etl_pipeline.writer;

import com.etl.etl_pipeline.config.SQLiteTransactionManager;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    
    @Autowired
    private SQLiteTransactionManager sqliteTransactionManager;
    
    // Lock to synchronize database writes
    private static final ReentrantLock dbLock = new ReentrantLock();
    
    // Retry configuration - read from application properties
    @Value("${spring.batch.retry.limit:10}")
    private int maxRetries;
    
    @Value("${spring.batch.retry.backoff.initial-interval:1000}")
    private long initialRetryDelayMs;
    
    @Value("${spring.batch.retry.backoff.multiplier:1.5}")
    private double backoffMultiplier;
    
    @Value("${spring.batch.retry.backoff.max-interval:15000}")
    private long maxRetryDelayMs;
    
    @Value("${spring.batch.lock.timeout-ms:10000}")
    private long lockTimeoutMs;

    private static final String INSERT_SQL = 
        "INSERT OR REPLACE INTO processed_data (" +
        "id, first_name, last_name, email, birth_date, address, city, country, " +
        "phone_number, salary, dependents, age, tax_rate, net_salary, full_name, " +
        "dependent_allowance, total_deductions, bonus, retirement_contribution, total_compensation, tax_amount, " +
        "processed_at, processing_status, validation_messages" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void write(@org.springframework.lang.NonNull Chunk<? extends ProcessedData> items) throws Exception {
        if (items.isEmpty()) {
            return;
        }
        
        log.info("Writing {} items to database", items.size());
        
        // Mark this as NOT a job repository operation to prioritize job repository operations
        SQLiteTransactionManager.clearJobRepositoryOperation();
        
        // Try to acquire lock with timeout to prevent deadlocks
        boolean lockAcquired = false;
        try {
            // Use a shorter timeout for data operations to prioritize job repository operations
            long effectiveLockTimeout = (long)(lockTimeoutMs * 0.8); // 80% of the configured timeout
            
            lockAcquired = dbLock.tryLock(effectiveLockTimeout, TimeUnit.MILLISECONDS);
            if (!lockAcquired) {
                log.warn("Could not acquire database write lock after {}ms, proceeding with caution", effectiveLockTimeout);
            }
            
            // Optimize database connection for batch operations
            optimizeDatabaseConnection();
            
            // Determine optimal batch size based on number of items
            // For larger batches, use smaller chunk sizes to reduce lock contention
            int optimalBatchSize = calculateOptimalBatchSize(items.size());
            
            // If we have more than one item, try batch processing
            if (items.size() > 1) {
                if (items.size() <= optimalBatchSize) {
                    // Process all items in a single batch if the size is reasonable
                    try {
                        writeBatchWithRetry(items);
                        return;
                    } catch (Exception e) {
                        log.warn("Batch insert failed, falling back to individual inserts: {}", e.getMessage());
                        // Fall back to individual inserts if batch fails
                    }
                } else {
                    // Split into smaller batches to reduce lock contention
                    log.info("Splitting {} items into smaller batches of {} items", items.size(), optimalBatchSize);
                    List<ProcessedData> batch = new ArrayList<>(optimalBatchSize);
                    int processed = 0;
                    
                    for (ProcessedData item : items) {
                        batch.add(item);
                        
                        if (batch.size() >= optimalBatchSize) {
                            try {
                                writeBatchWithRetry(new Chunk<>(batch));
                                processed += batch.size();
                                batch.clear();
                            } catch (Exception e) {
                                log.warn("Batch insert failed at item {}, falling back to individual inserts: {}", 
                                        processed, e.getMessage());
                                // Process remaining items individually
                                break;
                            }
                        }
                    }
                    
                    // Process any remaining items in the last batch
                    if (!batch.isEmpty()) {
                        try {
                            writeBatchWithRetry(new Chunk<>(batch));
                            processed += batch.size();
                            batch.clear();
                        } catch (Exception e) {
                            log.warn("Final batch insert failed, falling back to individual inserts for remaining items: {}", 
                                    e.getMessage());
                            // Process remaining items individually
                            for (ProcessedData data : batch) {
                                writeWithRetry(data);
                            }
                        }
                    }
                    
                    // If we've processed all items, we're done
                    if (processed == items.size()) {
                        return;
                    }
                }
            }
            
            // Individual inserts as fallback
            for (ProcessedData data : items) {
                writeWithRetry(data);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for database lock", e);
        } finally {
            // Always release the lock if we acquired it
            if (lockAcquired) {
                dbLock.unlock();
            }
        }
    }
    
    /**
     * Optimize database connection for batch operations
     */
    private void optimizeDatabaseConnection() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            log.warn("Cannot optimize database connection: DataSource is null");
            return;
        }
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Temporarily disable syncs for better performance during batch operations
            stmt.execute("PRAGMA synchronous = OFF");
            
            // Increase cache size temporarily for better performance
            stmt.execute("PRAGMA cache_size = -16000"); // 16MB
            
            // Use memory for temp storage
            stmt.execute("PRAGMA temp_store = MEMORY");
            
            // Increase the busy timeout for batch operations
            stmt.execute("PRAGMA busy_timeout = 120000"); // 120 seconds
            
            log.debug("Optimized database connection for batch operations");
        } catch (SQLException e) {
            log.warn("Failed to optimize database connection: {}", e.getMessage());
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
                    data.getSalary() != null ? data.getSalary() : 0.0,
                    data.getDependents() != null ? data.getDependents() : 0,
                    data.getAge() != null ? data.getAge() : 0,
                    data.getTaxRate() != null ? data.getTaxRate() : 0.0,
                    data.getNetSalary() != null ? data.getNetSalary() : 0.0,
                    data.getFullName(),
                    data.getDependentAllowance() != null ? data.getDependentAllowance() : 0.0,
                    data.getTotalDeductions() != null ? data.getTotalDeductions() : 0.0,
                    data.getBonus() != null ? data.getBonus() : 0.0,
                    data.getRetirementContribution() != null ? data.getRetirementContribution() : 0.0,
                    data.getTotalCompensation() != null ? data.getTotalCompensation() : 0.0,
                    data.getTaxAmount() != null ? data.getTaxAmount() : 0.0,
                    data.getProcessedAt() != null ? data.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    data.getProcessingStatus(),
                    data.getValidationMessages()
                );
                
                log.debug("Successfully wrote data for ID: {}", data.getId());
                success = true;
            } catch (UncategorizedSQLException e) {
                // Check if it's a database locked error
                if (e.getMessage() != null && (e.getMessage().contains("database is locked") || e.getMessage().contains("SQLITE_BUSY"))) {
                    log.warn("Database locked when writing data. Attempt {}/{}", attempts, maxRetries);
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
                    log.error("SQL error writing data: {}", e.getMessage());
                    throw e;
                }
            } catch (DataAccessException e) {
                log.error("Database error writing data: {}", e.getMessage());
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
            log.error("Failed to write data after {} attempts for ID: {}", maxRetries, data.getId());
            if (lastException != null) {
                throw lastException;
            } else {
                throw new RuntimeException("Failed to write data after " + maxRetries + " attempts");
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
                        ps.setDouble(10, data.getSalary() != null ? data.getSalary() : 0.0);
                        ps.setInt(11, data.getDependents() != null ? data.getDependents() : 0);
                        ps.setInt(12, data.getAge() != null ? data.getAge() : 0);
                        ps.setDouble(13, data.getTaxRate() != null ? data.getTaxRate() : 0.0);
                        ps.setDouble(14, data.getNetSalary() != null ? data.getNetSalary() : 0.0);
                        ps.setString(15, data.getFullName());
                        ps.setDouble(16, data.getDependentAllowance() != null ? data.getDependentAllowance() : 0.0);
                        ps.setDouble(17, data.getTotalDeductions() != null ? data.getTotalDeductions() : 0.0);
                        ps.setDouble(18, data.getBonus() != null ? data.getBonus() : 0.0);
                        ps.setDouble(19, data.getRetirementContribution() != null ? data.getRetirementContribution() : 0.0);
                        ps.setDouble(20, data.getTotalCompensation() != null ? data.getTotalCompensation() : 0.0);
                        ps.setDouble(21, data.getTaxAmount() != null ? data.getTaxAmount() : 0.0);
                        ps.setString(22, data.getProcessedAt() != null ? data.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                        ps.setString(23, data.getProcessingStatus() != null ? data.getProcessingStatus().toString() : null);
                        ps.setString(24, data.getValidationMessages());
                    }
                    
                    @Override
                    public int getBatchSize() {
                        return itemsRef.size();
                    }
                });
                
                log.info("Successfully wrote batch of {} items", items.size());
                success = true;
            } catch (UncategorizedSQLException e) {
                if (e.getMessage() != null && (e.getMessage().contains("database is locked") || e.getMessage().contains("SQLITE_BUSY"))) {
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
                    log.error("SQL error writing batch: {}", e.getMessage());
                    throw e;
                }
            } catch (DataAccessException e) {
                Throwable cause = e.getCause();
                boolean isLockError = false;
                
                while (cause != null) {
                    if (cause.getMessage() != null && 
                        (cause.getMessage().contains("database is locked") || 
                         cause.getMessage().contains("SQLITE_BUSY"))) {
                        isLockError = true;
                        break;
                    }
                    cause = cause.getCause();
                }
                
                if (isLockError) {
                    log.warn("Database locked (in cause chain) when writing batch. Attempt {}/{}", attempts, maxRetries);
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
                    log.error("Database error writing batch: {}", e.getMessage());
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
        // Use a percentage of the base delay for jitter (20%)
        delay += (long) (delay * 0.2 * Math.random());
        
        // Cap at max delay
        return Math.min(delay, maxRetryDelayMs);
    }
    
    /**
     * Calculate the optimal batch size based on the total number of items
     * This helps reduce lock contention by using smaller batches for larger datasets
     * 
     * @param totalItems The total number of items to process
     * @return The optimal batch size
     */
    private int calculateOptimalBatchSize(int totalItems) {
        // For very small batches, just process them all at once
        if (totalItems <= 10) {
            return totalItems;
        }
        
        // For larger batches, use a smaller size to reduce lock contention
        // The larger the batch, the smaller the chunk size (proportionally)
        if (totalItems <= 50) {
            return 10;
        } else if (totalItems <= 100) {
            return 20;
        } else if (totalItems <= 500) {
            return 25;
        } else if (totalItems <= 1000) {
            return 50;
        } else {
            return 100; // Cap at 100 for very large batches
        }
    }
    
    /**
     * Get database statistics for monitoring
     * @return String containing database statistics
     */
    public String getDatabaseStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Database Writer Stats:\n");
        stats.append("  - Writer Lock Status: ").append(dbLock.isLocked() ? "Locked" : "Unlocked").append("\n");
        stats.append("  - Writer Queue Length: ").append(dbLock.getQueueLength()).append("\n");
        stats.append("  - Max Retries: ").append(maxRetries).append("\n");
        stats.append("  - Initial Retry Delay: ").append(initialRetryDelayMs).append(" ms\n");
        stats.append("  - Backoff Multiplier: ").append(backoffMultiplier).append("\n");
        stats.append("  - Max Retry Delay: ").append(maxRetryDelayMs).append(" ms\n");
        stats.append("  - Lock Timeout: ").append(lockTimeoutMs).append(" ms\n");
        
        if (sqliteTransactionManager != null) {
            stats.append("  - Transaction Manager Status:\n");
            stats.append("    - Global Lock: ").append(sqliteTransactionManager.isDatabaseLocked() ? "Locked" : "Unlocked").append("\n");
            stats.append("    - Queue Length: ").append(sqliteTransactionManager.getQueueLength()).append("\n");
            stats.append("    - Active Transactions: ").append(sqliteTransactionManager.getActiveTransactionCount()).append("\n");
            stats.append("    - Lock Contention Count: ").append(sqliteTransactionManager.getLockContentionCount()).append("\n");
            stats.append("    - Total Transactions: ").append(sqliteTransactionManager.getTotalTransactionCount()).append("\n");
            stats.append("    - Failed Transactions: ").append(sqliteTransactionManager.getFailedTransactionCount()).append("\n");
            stats.append("    - Job Repository Operation: ").append(SQLiteTransactionManager.isJobRepositoryOperation()).append("\n");
        }
        
        return stats.toString();
    }
}
