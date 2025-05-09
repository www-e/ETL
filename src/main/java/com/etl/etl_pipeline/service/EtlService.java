package com.etl.etl_pipeline.service;

import com.etl.etl_pipeline.model.ProcessedData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for ETL operations
 */
@Slf4j
@Service
public class EtlService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job etlJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${etl.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${etl.output-dir:outputs}")
    private String outputDir;

    // Map to store job execution details
    private final Map<String, JobExecution> jobExecutions = new ConcurrentHashMap<>();

    /**
     * Process a file through the ETL pipeline
     * @param file File to process
     * @return Job ID
     */
    public String processFile(MultipartFile file) throws IOException, JobParametersInvalidException,
            JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        
        // Create upload directory if it doesn't exist
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
        
        // Create output directory structure if it doesn't exist
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        // Create type-specific output directories
        File csvOutputDir = new File(outputDir, "csv");
        File jsonOutputDir = new File(outputDir, "json");
        File excelOutputDir = new File(outputDir, "excel");
        
        if (!csvOutputDir.exists()) csvOutputDir.mkdirs();
        if (!jsonOutputDir.exists()) jsonOutputDir.mkdirs();
        if (!excelOutputDir.exists()) excelOutputDir.mkdirs();
        
        // Save the file
        String originalFilename = file.getOriginalFilename();
        String fileExtension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String newFilename = timestamp + "_" + originalFilename;
        
        // Save to upload directory first
        Path uploadPath = Paths.get(uploadDir, newFilename);
        Files.copy(file.getInputStream(), uploadPath);
        log.info("File saved to upload directory: {}", uploadPath);
        
        // Determine output directory based on file type
        File typeOutputDir;
        switch (fileExtension) {
            case "csv":
                typeOutputDir = csvOutputDir;
                break;
            case "json":
                typeOutputDir = jsonOutputDir;
                break;
            case "xls":
            case "xlsx":
                typeOutputDir = excelOutputDir;
                break;
            default:
                // Default to main output directory
                typeOutputDir = outputDirectory;
                break;
        }
        
        // Copy to type-specific output directory
        Path outputPath = Paths.get(typeOutputDir.getPath(), newFilename);
        Files.copy(uploadPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File copied to type-specific output directory: {}", outputPath);
        
        // Start the ETL job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", uploadPath.toString())
                .addString("fileType", fileExtension)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncher.run(etlJob, jobParameters);
        String jobId = String.valueOf(jobExecution.getJobId());
        
        // Store job execution for status tracking
        jobExecutions.put(jobId, jobExecution);
        
        log.info("ETL job started with ID: {}", jobId);
        return jobId;
    }

    /**
     * Get the status of an ETL job
     * @param jobId Job ID
     * @return Map with job status details
     */
    public Map<String, Object> getJobStatus(String jobId) {
        Map<String, Object> status = new HashMap<>();
        
        JobExecution jobExecution = jobExecutions.get(jobId);
        if (jobExecution == null) {
            status.put("status", "NOT_FOUND");
            status.put("message", "Job not found");
            return status;
        }
        
        BatchStatus batchStatus = jobExecution.getStatus();
        String exitCode = jobExecution.getExitStatus().getExitCode();
        
        status.put("jobId", jobId);
        status.put("status", batchStatus.toString());
        status.put("exitCode", exitCode);
        status.put("startTime", jobExecution.getStartTime());
        status.put("endTime", jobExecution.getEndTime());
        
        // Add job parameters including file information
        JobParameters jobParameters = jobExecution.getJobParameters();
        if (jobParameters != null) {
            status.put("jobParameters", jobParameters.getParameters());
            
            // Extract file information for convenience
            String filePath = jobParameters.getString("filePath");
            if (filePath != null) {
                // Extract just the filename from the path
                String fileName = filePath.contains("\\") ? 
                    filePath.substring(filePath.lastIndexOf("\\") + 1) : filePath;
                status.put("fileName", fileName);
            }
            
            String fileType = jobParameters.getString("fileType");
            if (fileType != null) {
                status.put("fileType", fileType);
            }
        }
        
        // Add step execution details
        List<Map<String, Object>> stepDetails = new ArrayList<>();
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Map<String, Object> step = new HashMap<>();
            step.put("stepName", stepExecution.getStepName());
            step.put("status", stepExecution.getStatus().toString());
            step.put("readCount", stepExecution.getReadCount());
            step.put("writeCount", stepExecution.getWriteCount());
            step.put("filterCount", stepExecution.getFilterCount());
            step.put("skipCount", stepExecution.getSkipCount());
            step.put("commitCount", stepExecution.getCommitCount());
            step.put("rollbackCount", stepExecution.getRollbackCount());
            
            stepDetails.add(step);
        }
        
        status.put("steps", stepDetails);
        
        return status;
    }

    /**
     * Get all processed data from the database
     * @return List of processed data
     */
    public List<ProcessedData> getAllProcessedData() {
        String sql = "SELECT * FROM processed_data";
        
        return jdbcTemplate.query(sql, getProcessedDataRowMapper());
    }

    /**
     * Get statistics about processed data
     * @return Map with statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count total records
        Integer totalRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM processed_data", Integer.class);
        stats.put("totalRecords", totalRecords != null ? totalRecords : 0);
        
        // Count valid records
        Integer validRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM processed_data WHERE processing_status = 'VALID'", Integer.class);
        stats.put("validRecords", validRecords != null ? validRecords : 0);
        
        // Count invalid records
        Integer invalidRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM processed_data WHERE processing_status = 'INVALID'", Integer.class);
        stats.put("invalidRecords", invalidRecords != null ? invalidRecords : 0);
        
        // Average age
        Double avgAge = jdbcTemplate.queryForObject(
            "SELECT AVG(age) FROM processed_data", Double.class);
        stats.put("averageAge", avgAge != null ? avgAge : 0);
        
        // Average salary
        Double avgSalary = jdbcTemplate.queryForObject(
            "SELECT AVG(salary) FROM processed_data", Double.class);
        stats.put("averageSalary", avgSalary != null ? avgSalary : 0);
        
        // Average net salary
        Double avgNetSalary = jdbcTemplate.queryForObject(
            "SELECT AVG(net_salary) FROM processed_data", Double.class);
        stats.put("averageNetSalary", avgNetSalary != null ? avgNetSalary : 0);
        
        // Count by country
        List<Map<String, Object>> countryStats = jdbcTemplate.queryForList(
            "SELECT country, COUNT(*) as count FROM processed_data GROUP BY country ORDER BY count DESC");
        stats.put("countryDistribution", countryStats);
        
        return stats;
    }

    /**
     * Preview raw data from an uploaded file
     * @param file File to preview
     * @return List of raw data records
     */
    public List<Map<String, Object>> previewFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> preview = new ArrayList<>();
        String filename = file.getOriginalFilename();
        
        if (filename == null) {
            return preview;
        }
        
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        
        switch (extension) {
            case "csv":
                preview = previewCsvFile(file);
                break;
            case "xlsx":
            case "xls":
                preview = previewExcelFile(file);
                break;
            case "json":
                preview = previewJsonFile(file);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + extension);
        }
        
        // Limit preview to 50 records
        return preview.size() > 50 ? preview.subList(0, 50) : preview;
    }

    /**
     * Preview CSV file
     * @param file CSV file
     * @return List of records
     */
    private List<Map<String, Object>> previewCsvFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().build())) {
            
            for (CSVRecord record : csvParser) {
                Map<String, Object> map = new HashMap<>();
                csvParser.getHeaderNames().forEach(header -> map.put(header, record.get(header)));
                records.add(map);
            }
        }
        
        return records;
    }

    /**
     * Preview Excel file
     * @param file Excel file
     * @return List of records
     */
    private List<Map<String, Object>> previewExcelFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            // Process data rows
            Iterator<Row> rowIterator = sheet.rowIterator();
            // Skip header
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, Object> record = new HashMap<>();
                
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    record.put(headers.get(i), cell != null ? getCellValueAsString(cell) : "");
                }
                
                records.add(record);
            }
        }
        
        return records;
    }

    /**
     * Preview JSON file
     * @param file JSON file
     * @return List of records
     */
    private List<Map<String, Object>> previewJsonFile(MultipartFile file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file.getInputStream(), 
            mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
    }

    /**
     * Get cell value as string
     * @param cell Excel cell
     * @return String value
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Row mapper for ProcessedData
     * @return RowMapper for ProcessedData
     */
    private RowMapper<ProcessedData> getProcessedDataRowMapper() {
        return (rs, rowNum) -> {
            ProcessedData data = new ProcessedData();
            
            data.setId(rs.getString("id"));
            data.setFirstName(rs.getString("first_name"));
            data.setLastName(rs.getString("last_name"));
            data.setEmail(rs.getString("email"));
            
            String birthDateStr = rs.getString("birth_date");
            data.setBirthDate(birthDateStr != null ? LocalDate.parse(birthDateStr) : null);
            
            data.setAddress(rs.getString("address"));
            data.setCity(rs.getString("city"));
            data.setCountry(rs.getString("country"));
            data.setPhoneNumber(rs.getString("phone_number"));
            data.setSalary(rs.getDouble("salary"));
            data.setDependents(rs.getInt("dependents"));
            data.setAge(rs.getInt("age"));
            data.setTaxRate(rs.getDouble("tax_rate"));
            data.setNetSalary(rs.getDouble("net_salary"));
            data.setFullName(rs.getString("full_name"));
            data.setDependentAllowance(rs.getDouble("dependent_allowance"));
            data.setTotalDeductions(rs.getDouble("total_deductions"));
            
            String processedAtStr = rs.getString("processed_at");
            data.setProcessedAt(processedAtStr != null ? 
                LocalDateTime.parse(processedAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            
            data.setProcessingStatus(rs.getString("processing_status"));
            data.setValidationMessages(rs.getString("validation_messages"));
            
            return data;
        };
    }
}
