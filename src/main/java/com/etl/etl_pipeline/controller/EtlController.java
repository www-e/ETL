package com.etl.etl_pipeline.controller;

import com.etl.etl_pipeline.model.ProcessedData;
import com.etl.etl_pipeline.service.EtlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for ETL operations
 */
@Slf4j
@RestController
@RequestMapping("/api/etl")
@CrossOrigin(origins = "*")
public class EtlController {

    @Autowired
    private EtlService etlService;

    /**
     * Upload a file and process it through the ETL pipeline
     * @param file File to process
     * @return Response with job execution details
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("Received file upload request: {}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Save the uploaded file and start ETL job
            String jobId = etlService.processFile(file);
            
            response.put("status", "success");
            response.put("message", "File uploaded and ETL job started");
            response.put("jobId", jobId);
            response.put("fileName", file.getOriginalFilename());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error handling file upload", e);
            response.put("status", "error");
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (JobParametersInvalidException | JobExecutionAlreadyRunningException | 
                JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.error("Error starting ETL job", e);
            response.put("status", "error");
            response.put("message", "Failed to start ETL job: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get the status of an ETL job
     * @param jobId Job ID
     * @return Response with job status
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        log.info("Checking status for job: {}", jobId);
        
        Map<String, Object> status = etlService.getJobStatus(jobId);
        return ResponseEntity.ok(status);
    }

    /**
     * Get all processed data
     * @return List of processed data
     */
    @GetMapping("/data")
    public ResponseEntity<List<ProcessedData>> getAllData() {
        log.info("Retrieving all processed data");
        
        List<ProcessedData> data = etlService.getAllProcessedData();
        return ResponseEntity.ok(data);
    }

    /**
     * Get summary statistics of processed data
     * @return Map of statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Retrieving ETL statistics");
        
        Map<String, Object> stats = etlService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Preview raw data from an uploaded file
     * @param file File to preview
     * @return List of raw data records
     */
    @PostMapping("/preview")
    public ResponseEntity<List<Map<String, Object>>> previewFile(@RequestParam("file") MultipartFile file) {
        log.info("Previewing file: {}", file.getOriginalFilename());
        
        try {
            List<Map<String, Object>> preview = etlService.previewFile(file);
            return ResponseEntity.ok(preview);
        } catch (IOException e) {
            log.error("Error previewing file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
