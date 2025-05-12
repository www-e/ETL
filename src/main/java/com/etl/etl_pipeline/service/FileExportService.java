package com.etl.etl_pipeline.service;

import com.etl.etl_pipeline.model.ProcessedData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting processed data to various file formats
 */
@Slf4j
@Service
public class FileExportService {

    @Value("${etl.output-dir:outputs}")
    private String outputDir;

    /**
     * Export processed data to a file
     * @param processedData List of processed data
     * @param originalFileName Original file name
     * @param fileType File type (csv, json, xlsx)
     * @return Path to the exported file
     */
    public String exportProcessedData(List<ProcessedData> processedData, String originalFileName, String fileType) {
        try {
            // Create timestamp for filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseFileName = timestamp + "_" + originalFileName;
            
            // Determine output directory based on file type
            String typeDir;
            switch (fileType.toLowerCase()) {
                case "csv":
                    typeDir = "csv";
                    return exportToCsv(processedData, baseFileName, typeDir);
                case "json":
                    typeDir = "json";
                    return exportToJson(processedData, baseFileName, typeDir);
                case "xls":
                case "xlsx":
                    typeDir = "excel";
                    return exportToExcel(processedData, baseFileName, typeDir);
                default:
                    log.warn("Unsupported file type for export: {}", fileType);
                    return null;
            }
        } catch (Exception e) {
            log.error("Error exporting processed data", e);
            return null;
        }
    }

    /**
     * Export processed data to CSV
     * @param processedData List of processed data
     * @param fileName File name
     * @param typeDir Type directory
     * @return Path to the exported file
     */
    private String exportToCsv(List<ProcessedData> processedData, String fileName, String typeDir) throws IOException {
        Path outputPath = getOutputPath(fileName, typeDir);
        
        // Define CSV headers
        String[] headers = {
            "ID", "First Name", "Last Name", "Email", "Birth Date", "Address", "City", "Country", 
            "Phone Number", "Salary", "Dependents", "Age", "Tax Rate", "Net Salary", "Full Name", 
            "Dependent Allowance", "Total Deductions", "Bonus", "Retirement Contribution", 
            "Total Compensation", "Tax Amount", "Processing Status"
        };
        
        try (FileWriter fileWriter = new FileWriter(outputPath.toFile());
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeader(headers))) {
            
            for (ProcessedData data : processedData) {
                csvPrinter.printRecord(
                    data.getId(),
                    data.getFirstName(),
                    data.getLastName(),
                    data.getEmail(),
                    data.getBirthDate(),
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
                    data.getBonus(),
                    data.getRetirementContribution(),
                    data.getTotalCompensation(),
                    data.getTaxAmount(),
                    data.getProcessingStatus()
                );
            }
            
            csvPrinter.flush();
            log.info("Exported processed data to CSV: {}", outputPath);
            return outputPath.toString();
        }
    }

    /**
     * Export processed data to JSON
     * @param processedData List of processed data
     * @param fileName File name
     * @param typeDir Type directory
     * @return Path to the exported file
     */
    private String exportToJson(List<ProcessedData> processedData, String fileName, String typeDir) throws IOException {
        Path outputPath = getOutputPath(fileName, typeDir);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), processedData);
        
        log.info("Exported processed data to JSON: {}", outputPath);
        return outputPath.toString();
    }

    /**
     * Export processed data to Excel
     * @param processedData List of processed data
     * @param fileName File name
     * @param typeDir Type directory
     * @return Path to the exported file
     */
    private String exportToExcel(List<ProcessedData> processedData, String fileName, String typeDir) throws IOException {
        Path outputPath = getOutputPath(fileName, typeDir);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Processed Data");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID", "First Name", "Last Name", "Email", "Birth Date", "Address", "City", "Country", 
                "Phone Number", "Salary", "Dependents", "Age", "Tax Rate", "Net Salary", "Full Name", 
                "Dependent Allowance", "Total Deductions", "Bonus", "Retirement Contribution", 
                "Total Compensation", "Tax Amount", "Processing Status"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // Create data rows
            int rowNum = 1;
            for (ProcessedData data : processedData) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(data.getId() != null ? data.getId() : "");
                row.createCell(1).setCellValue(data.getFirstName() != null ? data.getFirstName() : "");
                row.createCell(2).setCellValue(data.getLastName() != null ? data.getLastName() : "");
                row.createCell(3).setCellValue(data.getEmail() != null ? data.getEmail() : "");
                row.createCell(4).setCellValue(data.getBirthDate() != null ? data.getBirthDate().toString() : "");
                row.createCell(5).setCellValue(data.getAddress() != null ? data.getAddress() : "");
                row.createCell(6).setCellValue(data.getCity() != null ? data.getCity() : "");
                row.createCell(7).setCellValue(data.getCountry() != null ? data.getCountry() : "");
                row.createCell(8).setCellValue(data.getPhoneNumber() != null ? data.getPhoneNumber() : "");
                
                if (data.getSalary() != null) {
                    row.createCell(9).setCellValue(data.getSalary());
                }
                
                if (data.getDependents() != null) {
                    row.createCell(10).setCellValue(data.getDependents());
                }
                
                if (data.getAge() != null) {
                    row.createCell(11).setCellValue(data.getAge());
                }
                
                if (data.getTaxRate() != null) {
                    row.createCell(12).setCellValue(data.getTaxRate());
                }
                
                if (data.getNetSalary() != null) {
                    row.createCell(13).setCellValue(data.getNetSalary());
                }
                
                row.createCell(14).setCellValue(data.getFullName() != null ? data.getFullName() : "");
                
                if (data.getDependentAllowance() != null) {
                    row.createCell(15).setCellValue(data.getDependentAllowance());
                }
                
                if (data.getTotalDeductions() != null) {
                    row.createCell(16).setCellValue(data.getTotalDeductions());
                }
                
                if (data.getBonus() != null) {
                    row.createCell(17).setCellValue(data.getBonus());
                }
                
                if (data.getRetirementContribution() != null) {
                    row.createCell(18).setCellValue(data.getRetirementContribution());
                }
                
                if (data.getTotalCompensation() != null) {
                    row.createCell(19).setCellValue(data.getTotalCompensation());
                }
                
                if (data.getTaxAmount() != null) {
                    row.createCell(20).setCellValue(data.getTaxAmount());
                }
                
                row.createCell(21).setCellValue(data.getProcessingStatus() != null ? data.getProcessingStatus() : "");
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputPath.toFile())) {
                workbook.write(outputStream);
            }
            
            log.info("Exported processed data to Excel: {}", outputPath);
            return outputPath.toString();
        }
    }

    /**
     * Get output path for a file
     * @param fileName File name
     * @param typeDir Type directory
     * @return Output path
     */
    private Path getOutputPath(String fileName, String typeDir) throws IOException {
        // Create output directory if it doesn't exist
        Path typeDirPath = Paths.get(outputDir, typeDir);
        if (!Files.exists(typeDirPath)) {
            Files.createDirectories(typeDirPath);
        }
        
        return Paths.get(typeDirPath.toString(), fileName);
    }
}
