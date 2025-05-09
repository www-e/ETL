package com.etl.etl_pipeline.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Custom SQLite transaction manager to handle SQLite-specific transaction behavior
 * with enhanced handling for database locks
 */
@Slf4j
@Component
public class SQLiteTransactionManager extends DataSourceTransactionManager {

    private static final ReentrantLock globalLock = new ReentrantLock(true); // Fair lock

    private static final ThreadLocal<Boolean> isJobRepositoryOperation = new ThreadLocal<>();

    // Track active transactions and lock contention
    private static final AtomicInteger activeTransactions = new AtomicInteger(0);
    private static final AtomicInteger lockContentions = new AtomicInteger(0);
    private static final Map<String, Integer> lockContentionsByOperation = new ConcurrentHashMap<>();
    private static final AtomicInteger totalTransactions = new AtomicInteger(0);
    private static final AtomicInteger failedTransactions = new AtomicInteger(0);

    @Value("${sqlite.lock.timeout-ms:30000}")
    private long lockTimeoutMs = 30000;

    @Value("${sqlite.transaction.retry-count:10}")
    private int maxRetryCount = 10;

    @Value("${sqlite.transaction.backoff-ms:200}")
    private long backoffMs = 200;
    
    @Value("${sqlite.transaction.max-backoff-ms:5000}")
    private long maxBackoffMs = 5000;
    
    @Value("${sqlite.transaction.backoff-multiplier:2}")
    private int backoffMultiplier = 2;

    @Value("${sqlite.job-repository.priority:true}")
    private boolean jobRepositoryPriority = true;

    public SQLiteTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void doBegin(@NonNull Object transaction, @NonNull TransactionDefinition definition) {
        totalTransactions.incrementAndGet();
        activeTransactions.incrementAndGet();
        String operationName = getOperationName();

        int retryCount = 0;
        boolean acquired = false;
        long startTime = System.currentTimeMillis();

        // Give priority to job repository operations if configured
        long currentLockTimeoutMs = lockTimeoutMs;
        if (jobRepositoryPriority && isJobRepositoryOperation()) {
            // Job repository operations get longer timeout
            currentLockTimeoutMs = lockTimeoutMs * 2;
            log.debug("Job repository operation detected, using extended lock timeout: {}ms", currentLockTimeoutMs);
        }

        while (!acquired && retryCount < maxRetryCount) {
            try {
                // Try to acquire the global lock with timeout
                boolean lockAcquired = false;

                // Job repository operations get priority if configured
                if (jobRepositoryPriority && isJobRepositoryOperation() && globalLock.hasQueuedThreads()) {
                    // For job repository operations, we use a longer timeout and log the contention
                    lockAcquired = globalLock.tryLock(currentLockTimeoutMs, TimeUnit.MILLISECONDS);
                    if (!lockAcquired) {
                        log.warn("Job repository operation could not acquire lock despite priority");
                    }
                } else {
                    // Regular lock acquisition
                    lockAcquired = globalLock.tryLock(currentLockTimeoutMs, TimeUnit.MILLISECONDS);
                }

                if (lockAcquired) {
                    try {
                        // Once we have the lock, proceed with the transaction
                        super.doBegin(transaction, definition);

                        // Get connection from the transaction
                        Connection connection = null;
                        if (transaction instanceof DefaultTransactionStatus) {
                            try {
                                // Get connection using DataSourceUtils
                                DataSource dataSource = getDataSource();
                                if (dataSource != null) {
                                    connection = DataSourceUtils.getConnection(dataSource);
                                }
                            } catch (Exception e) {
                                log.warn("Could not get connection from transaction: {}", e.getMessage());
                            }
                        }
                        
                        if (connection != null) {
                            try {
                                // Set SQLite-specific pragmas for better concurrency
                                try (Statement stmt = connection.createStatement()) {
                                    // Set journal mode to WAL for better concurrency
                                    stmt.execute("PRAGMA journal_mode=WAL");
                                    
                                    // Set busy timeout dynamically based on our configuration
                                    stmt.execute("PRAGMA busy_timeout=" + lockTimeoutMs);
                                    log.debug("Set SQLite busy_timeout to {}ms", lockTimeoutMs);
                                    
                                    // Set synchronous mode to NORMAL for better performance
                                    stmt.execute("PRAGMA synchronous=NORMAL");
                                    
                                    // Enable foreign keys
                                    stmt.execute("PRAGMA foreign_keys=ON");
                                    
                                    // Additional optimizations for better concurrency
                                    stmt.execute("PRAGMA temp_store=MEMORY");
                                    stmt.execute("PRAGMA cache_size=10000");
                                }
                            } catch (SQLException e) {
                                log.warn("Failed to set SQLite pragmas: {}", e.getMessage());
                            }
                        }

                        acquired = true;
                        log.debug("Transaction started successfully for operation: {}", operationName);
                    } finally {
                        // Always release the lock
                        globalLock.unlock();
                    }
                } else {
                    // Lock acquisition timed out - record contention
                    lockContentions.incrementAndGet();
                    lockContentionsByOperation.compute(operationName, (k, v) -> (v == null) ? 1 : v + 1);

                    log.warn("Failed to acquire global lock for {} after {}ms, retrying... (attempt {}/{})", 
                            operationName, currentLockTimeoutMs, retryCount + 1, maxRetryCount);
                    retryCount++;

                    // Wait before retrying with exponential backoff and jitter
                    if (retryCount < maxRetryCount) {
                        long delay = calculateRetryDelay(retryCount);
                        Thread.sleep(delay);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failedTransactions.incrementAndGet();
                activeTransactions.decrementAndGet();
                throw new RuntimeException("Thread interrupted while waiting for database lock", e);
            } catch (Exception e) {
                // Check if this is a locking-related exception
                boolean isLock = isLockException(e);
                if (isLock) {
                    log.warn("Database lock error during transaction begin (retry {}/{}): {}", 
                            retryCount + 1, maxRetryCount, e.getMessage());
                    
                    // For lock errors, we'll retry with backoff
                    lockContentions.incrementAndGet();
                    lockContentionsByOperation.compute(operationName, (k, v) -> (v == null) ? 1 : v + 1);
                    
                    retryCount++;
                    
                    // Wait before retrying with exponential backoff and jitter
                    if (retryCount < maxRetryCount) {
                        try {
                            long delay = calculateRetryDelay(retryCount);
                            log.info("Waiting {}ms before retry {} for operation: {}", 
                                    delay, retryCount, operationName);
                            Thread.sleep(delay);
                            // Continue to next retry iteration
                            continue;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Thread interrupted during backoff", ie);
                        }
                    }
                } else {
                    // For non-lock errors, log and fail immediately
                    log.error("Unexpected error during transaction begin: {}", e.getMessage(), e);
                }
                
                failedTransactions.incrementAndGet();
                activeTransactions.decrementAndGet();
                throw new RuntimeException("Failed to begin transaction: " + e.getMessage(), e);
            }
        }

        if (!acquired) {
            failedTransactions.incrementAndGet();
            activeTransactions.decrementAndGet();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("Failed to acquire database lock after {} attempts and {}ms for operation: {}", 
                    maxRetryCount, elapsedTime, operationName);
            throw new RuntimeException("Failed to acquire database lock after " + maxRetryCount + 
                    " attempts and " + elapsedTime + "ms for operation: " + operationName);
        }
    }

    /**
     * Calculate retry delay with exponential backoff and jitter
     * Uses a progressive backoff strategy with a multiplier and maximum cap
     * @param retryCount Current retry attempt number
     * @return Delay in milliseconds to wait before next retry
     */
    private long calculateRetryDelay(int retryCount) {
        // Exponential backoff with jitter
        long baseDelay = backoffMs * (long) Math.pow(backoffMultiplier, retryCount - 1);
        // Cap at maximum backoff
        baseDelay = Math.min(baseDelay, maxBackoffMs);
        // Add jitter (20%) to prevent synchronized retries
        long jitter = (long) (baseDelay * 0.2 * Math.random());
        
        log.debug("Calculated retry delay: {}ms (retry {}/{})", baseDelay + jitter, retryCount, maxRetryCount);
        return baseDelay + jitter;
    }

    /**
     * Get a descriptive name for the current operation
     */
    private String getOperationName() {
        if (isJobRepositoryOperation()) {
            return "JobRepository";
        }

        // Try to determine the operation from the stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("Writer") || className.contains("Reader")) {
                return className.substring(className.lastIndexOf('.') + 1) + "." + element.getMethodName();
            }
        }

        return "UnknownOperation";
    }

    @Override
    protected void doCommit(@NonNull DefaultTransactionStatus status) {
        String operationName = getOperationName();
        try {
            // Try to acquire the global lock with timeout
            long currentLockTimeoutMs = lockTimeoutMs;
            if (jobRepositoryPriority && isJobRepositoryOperation()) {
                // Job repository operations get longer timeout
                currentLockTimeoutMs = lockTimeoutMs * 2;
            }

            if (globalLock.tryLock(currentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
                try {
                    super.doCommit(status);
                    log.debug("Transaction committed successfully for operation: {}", operationName);
                } finally {
                    globalLock.unlock();
                }
            } else {
                // Lock acquisition timed out - record contention
                lockContentions.incrementAndGet();
                lockContentionsByOperation.compute(operationName, (k, v) -> (v == null) ? 1 : v + 1);

                log.error("Failed to acquire lock for transaction commit after {}ms for operation: {}", 
                        currentLockTimeoutMs, operationName);
                failedTransactions.incrementAndGet();
                throw new RuntimeException("Failed to acquire lock for transaction commit after " + 
                        currentLockTimeoutMs + "ms for operation: " + operationName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedTransactions.incrementAndGet();
            throw new RuntimeException("Thread interrupted while waiting for database lock during commit", e);
        } finally {
            activeTransactions.decrementAndGet();
        }
    }

    @Override
    protected void doRollback(@NonNull DefaultTransactionStatus status) {
        String operationName = getOperationName();
        try {
            // Try to acquire the global lock with timeout
            long currentLockTimeoutMs = lockTimeoutMs;
            if (jobRepositoryPriority && isJobRepositoryOperation()) {
                // Job repository operations get longer timeout
                currentLockTimeoutMs = lockTimeoutMs * 2;
            }
            
            if (globalLock.tryLock(currentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
                try {
                    super.doRollback(status);
                    log.debug("Transaction rolled back successfully for operation: {}", operationName);
                } finally {
                    globalLock.unlock();
                }
            } else {
                // Lock acquisition timed out - record contention
                lockContentions.incrementAndGet();
                lockContentionsByOperation.compute(operationName, (k, v) -> (v == null) ? 1 : v + 1);
                
                log.error("Failed to acquire lock for transaction rollback after {}ms for operation: {}", 
                        currentLockTimeoutMs, operationName);
                failedTransactions.incrementAndGet();
                throw new RuntimeException("Failed to acquire lock for transaction rollback after " + 
                        currentLockTimeoutMs + "ms for operation: " + operationName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedTransactions.incrementAndGet();
            throw new RuntimeException("Thread interrupted while waiting for database lock during rollback", e);
        } finally {
            activeTransactions.decrementAndGet();
        }
    }

    /**
     * Mark the current thread as performing a job repository operation
     */
    public static void markAsJobRepositoryOperation() {
        isJobRepositoryOperation.set(true);
    }

    /**
     * Clear the job repository operation flag
     */
    public static void clearJobRepositoryOperation() {
        isJobRepositoryOperation.remove();
    }

    /**
     * Check if the current thread is performing a job repository operation
     */
    public static boolean isJobRepositoryOperation() {
        Boolean isJobRepo = isJobRepositoryOperation.get();
        return isJobRepo != null && isJobRepo;
    }

    /**
     * Get the current number of active transactions
     */
    public int getActiveTransactionCount() {
        return activeTransactions.get();
    }

    /**
     * Get the current lock contention count
     */
    public int getLockContentionCount() {
        return lockContentions.get();
    }

    /**
     * Get lock contentions by operation
     */
    public Map<String, Integer> getLockContentionsByOperation() {
        return new ConcurrentHashMap<>(lockContentionsByOperation);
    }

    /**
     * Get total transaction count
     */
    public int getTotalTransactionCount() {
        return totalTransactions.get();
    }

    /**
     * Get failed transaction count
     */
    public int getFailedTransactionCount() {
        return failedTransactions.get();
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        lockContentions.set(0);
        lockContentionsByOperation.clear();
        totalTransactions.set(0);
        failedTransactions.set(0);
    }

    /**
     * Check if an exception is related to a database lock
     */
    public boolean isLockException(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (
                message.contains("database is locked") ||
                message.contains("SQLITE_BUSY") ||
                message.contains("database lock") ||
                message.contains("cannot start a transaction")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
    
    /**
     * Check if the database is currently locked
     * @return true if locked, false otherwise
     */
    public boolean isDatabaseLocked() {
        return globalLock.isLocked();
    }
    
    /**
     * Get the current number of threads waiting for the database lock
     * @return number of waiting threads
     */
    public int getQueueLength() {
        return globalLock.getQueueLength();
    }
    
    /**
     * Get detailed database status information
     * @return String containing database status information
     */
    public String getDatabaseStatus() {
        StringBuilder status = new StringBuilder();
        status.append("SQLite Transaction Manager Status:\n");
        status.append("  - Global Lock: ").append(isDatabaseLocked() ? "Locked" : "Unlocked").append("\n");
        status.append("  - Queue Length: ").append(getQueueLength()).append("\n");
        status.append("  - Active Transactions: ").append(getActiveTransactionCount()).append("\n");
        status.append("  - Lock Contention Count: ").append(getLockContentionCount()).append("\n");
        status.append("  - Total Transactions: ").append(getTotalTransactionCount()).append("\n");
        status.append("  - Failed Transactions: ").append(getFailedTransactionCount()).append("\n");
        status.append("  - Job Repository Operation: ").append(isJobRepositoryOperation()).append("\n");
        
        // Add lock contention by operation
        Map<String, Integer> contentions = getLockContentionsByOperation();
        if (!contentions.isEmpty()) {
            status.append("  - Lock Contentions By Operation:\n");
            contentions.forEach((operation, count) -> 
                status.append("    - ").append(operation).append(": ").append(count).append("\n"));
        }
        
        return status.toString();
    }
}
