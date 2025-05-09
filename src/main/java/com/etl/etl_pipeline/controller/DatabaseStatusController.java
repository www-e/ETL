package com.etl.etl_pipeline.controller;

import com.etl.etl_pipeline.config.SQLiteTransactionManager;
import com.etl.etl_pipeline.writer.DatabaseWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for monitoring database status and health
 */
@RestController
@RequestMapping("/api/database")
public class DatabaseStatusController {

    private final SQLiteTransactionManager sqliteTransactionManager;
    private final DatabaseWriter databaseWriter;

    @Autowired
    public DatabaseStatusController(SQLiteTransactionManager sqliteTransactionManager, DatabaseWriter databaseWriter) {
        this.sqliteTransactionManager = sqliteTransactionManager;
        this.databaseWriter = databaseWriter;
    }

    /**
     * Get current database status information
     * @return Database status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Get lock status from transaction manager
        status.put("isLocked", sqliteTransactionManager.isDatabaseLocked());
        status.put("queueLength", sqliteTransactionManager.getQueueLength());
        status.put("activeTransactions", sqliteTransactionManager.getActiveTransactionCount());
        status.put("lockContentions", sqliteTransactionManager.getLockContentionCount());
        status.put("totalTransactions", sqliteTransactionManager.getTotalTransactionCount());
        status.put("failedTransactions", sqliteTransactionManager.getFailedTransactionCount());
        status.put("isJobRepositoryOperation", SQLiteTransactionManager.isJobRepositoryOperation());
        
        // Get detailed stats from database writer
        status.put("writerStats", databaseWriter.getDatabaseStats());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Reset database statistics
     * @return Success message
     */
    @PostMapping("/reset-stats")
    public ResponseEntity<Map<String, Object>> resetDatabaseStats() {
        Map<String, Object> response = new HashMap<>();
        
        // Reset statistics in transaction manager
        sqliteTransactionManager.resetStatistics();
        
        response.put("success", true);
        response.put("message", "Database statistics have been reset");
        
        return ResponseEntity.ok(response);
    }
}
