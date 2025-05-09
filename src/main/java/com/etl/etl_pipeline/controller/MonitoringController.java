package com.etl.etl_pipeline.controller;

import com.etl.etl_pipeline.config.SQLiteTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for monitoring database and application health
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitoringController {

    private final SQLiteTransactionManager transactionManager;

    @Autowired
    public MonitoringController(SQLiteTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Get database status information
     * @return Map containing database status metrics
     */
    @GetMapping("/db-status")
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Add basic status information
        status.put("isLocked", transactionManager.isDatabaseLocked());
        status.put("queueLength", transactionManager.getQueueLength());
        status.put("activeTransactions", transactionManager.getActiveTransactionCount());
        status.put("lockContentions", transactionManager.getLockContentionCount());
        status.put("totalTransactions", transactionManager.getTotalTransactionCount());
        status.put("failedTransactions", transactionManager.getFailedTransactionCount());
        status.put("lockContentionsByOperation", transactionManager.getLockContentionsByOperation());
        
        // Log the status for server-side monitoring
        log.info("Database status: {}", status);
        
        return status;
    }
    
    /**
     * Reset database statistics
     * @return Map containing confirmation message
     */
    @GetMapping("/reset-stats")
    public Map<String, Object> resetDatabaseStats() {
        Map<String, Object> result = new HashMap<>();
        
        transactionManager.resetStatistics();
        result.put("message", "Database statistics reset successfully");
        log.info("Database statistics reset");
        
        return result;
    }
}
