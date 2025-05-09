package com.etl.etl_pipeline.controller;

import com.etl.etl_pipeline.config.SQLiteTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/job-history")
public class JobHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(JobHistoryController.class);

    private final JobExplorer jobExplorer;
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${job.history.max-retries:5}")
    private int maxRetries = 5;
    
    @Value("${job.history.retry-delay-ms:500}")
    private long retryDelayMs = 500;

    @Autowired
    public JobHistoryController(JobExplorer jobExplorer, JdbcTemplate jdbcTemplate) {
        this.jobExplorer = jobExplorer;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get all job history with retry logic for handling database locks
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllJobs() {
        // Mark this as a job repository operation to prioritize it
        SQLiteTransactionManager.markAsJobRepositoryOperation();
        
        try {
            return getJobsWithRetry();
        } catch (Exception e) {
            logger.error("Failed to retrieve job history after {} retries: {}", maxRetries, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", "Failed to retrieve job history: " + e.getMessage())));
        } finally {
            // Clear the job repository operation flag
            SQLiteTransactionManager.clearJobRepositoryOperation();
        }
    }
    
    /**
     * Get jobs with retry logic for handling database locks
     */
    private ResponseEntity<List<Map<String, Object>>> getJobsWithRetry() throws InterruptedException {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            attempts++;
            try {
                List<String> jobNames = jobExplorer.getJobNames();
                List<Map<String, Object>> result = new ArrayList<>();

                for (String jobName : jobNames) {
                    List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(jobName, 0, 100);
                    
                    for (JobInstance jobInstance : jobInstances) {
                        List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                        
                        for (JobExecution jobExecution : jobExecutions) {
                            Map<String, Object> jobData = new HashMap<>();
                            jobData.put("jobId", jobExecution.getId());
                            jobData.put("jobName", jobName);
                            jobData.put("jobInstanceId", jobInstance.getId());
                            jobData.put("startTime", jobExecution.getStartTime());
                            jobData.put("endTime", jobExecution.getEndTime());
                            jobData.put("status", jobExecution.getStatus().toString());
                            jobData.put("exitCode", jobExecution.getExitStatus().getExitCode());
                            jobData.put("exitMessage", jobExecution.getExitStatus().getExitDescription());
                            
                            // Extract job parameters
                            Map<String, Object> parameters = new HashMap<>();
                            jobExecution.getJobParameters().getParameters().forEach((key, value) -> 
                                parameters.put(key, value.getValue()));
                            jobData.put("parameters", parameters);
                            
                            // Add step execution information
                            jobData.put("stepExecutions", getStepExecutionDetails(jobExecution));
                            
                            result.add(jobData);
                        }
                    }
                }
                
                logger.info("Successfully retrieved {} job executions", result.size());
                return ResponseEntity.ok(result);
                
            } catch (Exception e) {
                lastException = e;
                
                // Check if it's a database lock error
                boolean isLockError = isLockException(e);
                
                if (isLockError && attempts < maxRetries) {
                    long delay = calculateRetryDelay(attempts);
                    logger.warn("Database locked when retrieving job history. Retry {}/{} after {}ms", 
                            attempts, maxRetries, delay);
                    TimeUnit.MILLISECONDS.sleep(delay);
                } else {
                    // If it's not a lock error or we've exceeded retries, rethrow
                    logger.error("Error retrieving job history: {}", e.getMessage());
                    throw new RuntimeException("Failed to retrieve job history", e);
                }
            }
        }
        
        // If we've exhausted all retries, throw the last exception
        throw new RuntimeException("Failed to retrieve job history after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Get step execution details for a job execution
     */
    private List<Map<String, Object>> getStepExecutionDetails(JobExecution jobExecution) {
        List<Map<String, Object>> stepDetails = new ArrayList<>();
        
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            Map<String, Object> stepData = new HashMap<>();
            stepData.put("stepName", stepExecution.getStepName());
            stepData.put("status", stepExecution.getStatus().toString());
            stepData.put("readCount", stepExecution.getReadCount());
            stepData.put("writeCount", stepExecution.getWriteCount());
            stepData.put("commitCount", stepExecution.getCommitCount());
            stepData.put("rollbackCount", stepExecution.getRollbackCount());
            stepData.put("startTime", stepExecution.getStartTime());
            stepData.put("endTime", stepExecution.getEndTime());
            stepData.put("exitCode", stepExecution.getExitStatus().getExitCode());
            stepData.put("exitMessage", stepExecution.getExitStatus().getExitDescription());
            
            stepDetails.add(stepData);
        });
        
        return stepDetails;
    }

    /**
     * Delete a job with retry logic for handling database locks
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable Long jobId) {
        // Mark this as a job repository operation to prioritize it
        SQLiteTransactionManager.markAsJobRepositoryOperation();
        
        try {
            return deleteJobWithRetry(jobId);
        } catch (Exception e) {
            logger.error("Failed to delete job after {} retries: {}", maxRetries, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting job: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            // Clear the job repository operation flag
            SQLiteTransactionManager.clearJobRepositoryOperation();
        }
    }
    
    /**
     * Delete job with retry logic for handling database locks
     */
    private ResponseEntity<Map<String, Object>> deleteJobWithRetry(Long jobId) throws InterruptedException {
        int attempts = 0;
        Exception lastException = null;
        Map<String, Object> response = new HashMap<>();
        
        while (attempts < maxRetries) {
            attempts++;
            try {
                // First, check if the job exists
                JobExecution jobExecution = jobExplorer.getJobExecution(jobId);
                if (jobExecution == null) {
                    response.put("success", false);
                    response.put("message", "Job not found with ID: " + jobId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
                
                // Get job instance ID before deleting
                Long jobInstanceId = jobExecution.getJobInstance().getId();
                
                // Delete in a specific order to maintain referential integrity
                // 1. Delete step execution context
                jdbcTemplate.update(
                    "DELETE FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN " +
                    "(SELECT STEP_EXECUTION_ID FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?)", 
                    jobId
                );
                
                // 2. Delete step executions
                jdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?", jobId);
                
                // 3. Delete job execution context
                jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = ?", jobId);
                
                // 4. Delete job execution params
                jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID = ?", jobId);
                
                // 5. Delete job execution
                jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?", jobId);
                
                // Check if there are any other job executions for this instance
                Integer remainingExecutions = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = ?", 
                    Integer.class, 
                    jobInstanceId
                );
                
                // If no other executions exist, delete the job instance
                if (remainingExecutions != null && remainingExecutions == 0) {
                    jdbcTemplate.update("DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID = ?", jobInstanceId);
                }
                
                logger.info("Successfully deleted job with ID: {}", jobId);
                response.put("success", true);
                response.put("message", "Job deleted successfully");
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                lastException = e;
                
                // Check if it's a database lock error
                boolean isLockError = isLockException(e);
                
                if (isLockError && attempts < maxRetries) {
                    long delay = calculateRetryDelay(attempts);
                    logger.warn("Database locked when deleting job {}. Retry {}/{} after {}ms", 
                            jobId, attempts, maxRetries, delay);
                    TimeUnit.MILLISECONDS.sleep(delay);
                } else {
                    // If it's not a lock error or we've exceeded retries, rethrow
                    logger.error("Error deleting job {}: {}", jobId, e.getMessage());
                    throw new RuntimeException("Failed to delete job " + jobId, e);
                }
            }
        }
        
        // If we've exhausted all retries, throw the last exception
        throw new RuntimeException("Failed to delete job " + jobId + " after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Calculate retry delay with exponential backoff and jitter
     */
    private long calculateRetryDelay(int attempt) {
        // Exponential backoff with jitter
        long baseDelay = retryDelayMs * (long) Math.pow(2, attempt - 1);
        long jitter = (long) (baseDelay * 0.2 * Math.random()); // 20% jitter
        return baseDelay + jitter;
    }
    
    /**
     * Check if the exception is related to a database lock
     */
    private boolean isLockException(Exception ex) {
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
}
