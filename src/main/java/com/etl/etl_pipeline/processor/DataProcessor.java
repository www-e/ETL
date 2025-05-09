package com.etl.etl_pipeline.processor;

import com.etl.etl_pipeline.model.InputData;
import com.etl.etl_pipeline.model.ProcessedData;
import com.etl.etl_pipeline.util.DateUtils;
import com.etl.etl_pipeline.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for transforming input data into processed data
 * Applies validation, cleaning, and mathematical calculations
 */
@Slf4j
@Component
public class DataProcessor implements ItemProcessor<InputData, ProcessedData> {

    @Override
    public ProcessedData process(@org.springframework.lang.NonNull InputData item) throws Exception {
        log.info("Processing data for: {}", item.getId());
        
        // Validate and collect validation messages
        List<String> validationMessages = new ArrayList<>();
        boolean isValid = validateData(item, validationMessages);
        
        // Clean data
        cleanData(item);
        
        // Apply mathematical calculations
        applyCalculations(item);
        
        // Map to processed data
        ProcessedData processedData = mapToProcessedData(item);
        
        // Set processing status and validation messages
        processedData.setProcessingStatus(isValid ? "VALID" : "INVALID");
        processedData.setValidationMessages(String.join("; ", validationMessages));
        processedData.setProcessedAt(LocalDateTime.now());
        
        log.info("Processed data for: {}. Status: {}", item.getId(), processedData.getProcessingStatus());
        return processedData;
    }
    
    /**
     * Validates the input data and collects validation messages
     * @param data Input data to validate
     * @param validationMessages List to collect validation messages
     * @return True if data is valid, false otherwise
     */
    private boolean validateData(InputData data, List<String> validationMessages) {
        boolean isValid = true;
        
        // Check for null or empty required fields
        if (data.getId() == null || data.getId().trim().isEmpty()) {
            validationMessages.add("ID is required");
            isValid = false;
        }
        
        // Validate email format
        if (!ValidationUtils.isValidEmail(data.getEmail())) {
            validationMessages.add("Invalid email format");
            isValid = false;
        }
        
        // Validate birth date
        if (data.getBirthDate() == null) {
            validationMessages.add("Birth date is required");
            isValid = false;
        } else if (data.getBirthDate().isAfter(java.time.LocalDate.now())) {
            validationMessages.add("Birth date cannot be in the future");
            isValid = false;
        }
        
        // Validate salary
        if (data.getSalary() == null || data.getSalary() < 0) {
            validationMessages.add("Salary must be a positive number");
            isValid = false;
        }
        
        // Validate dependents
        if (data.getDependents() == null || data.getDependents() < 0) {
            validationMessages.add("Dependents must be a non-negative integer");
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Cleans the input data by trimming strings, normalizing values, etc.
     * @param data Input data to clean
     */
    private void cleanData(InputData data) {
        // Trim string fields
        if (data.getFirstName() != null) data.setFirstName(data.getFirstName().trim());
        if (data.getLastName() != null) data.setLastName(data.getLastName().trim());
        if (data.getEmail() != null) data.setEmail(data.getEmail().trim().toLowerCase());
        if (data.getAddress() != null) data.setAddress(data.getAddress().trim());
        if (data.getCity() != null) data.setCity(data.getCity().trim());
        if (data.getCountry() != null) data.setCountry(data.getCountry().trim());
        if (data.getPhoneNumber() != null) data.setPhoneNumber(data.getPhoneNumber().trim());
        
        // Normalize phone number (remove non-numeric characters)
        if (data.getPhoneNumber() != null) {
            data.setPhoneNumber(data.getPhoneNumber().replaceAll("[^0-9+]", ""));
        }
        
        // Set default values for null fields
        if (data.getSalary() == null) data.setSalary(0.0);
        if (data.getDependents() == null) data.setDependents(0);
    }
    
    /**
     * Applies mathematical calculations to the input data
     * @param data Input data to process
     */
    private void applyCalculations(InputData data) {
        // Calculate age
        if (data.getBirthDate() != null) {
            data.setAge(DateUtils.calculateAge(data.getBirthDate()));
        }
        
        // Calculate tax rate based on salary
        // Example tax brackets:
        // 0-20000: 10%
        // 20001-50000: 15%
        // 50001-100000: 20%
        // 100001+: 25%
        double salary = data.getSalary() != null ? data.getSalary() : 0.0;
        double taxRate;
        
        if (salary <= 20000) {
            taxRate = 0.10;
        } else if (salary <= 50000) {
            taxRate = 0.15;
        } else if (salary <= 100000) {
            taxRate = 0.20;
        } else {
            taxRate = 0.25;
        }
        
        data.setTaxRate(taxRate);
        
        // Calculate net salary after tax
        double netSalary = salary * (1 - taxRate);
        data.setNetSalary(netSalary);
    }
    
    /**
     * Maps input data to processed data
     * @param data Input data to map
     * @return Processed data
     */
    private ProcessedData mapToProcessedData(InputData data) {
        ProcessedData processedData = ProcessedData.builder()
            .id(data.getId())
            .firstName(data.getFirstName())
            .lastName(data.getLastName())
            .email(data.getEmail())
            .birthDate(data.getBirthDate())
            .address(data.getAddress())
            .city(data.getCity())
            .country(data.getCountry())
            .phoneNumber(data.getPhoneNumber())
            .salary(data.getSalary())
            .dependents(data.getDependents())
            .age(data.getAge())
            .taxRate(data.getTaxRate())
            .netSalary(data.getNetSalary())
            .build();
        
        // Calculate additional fields
        processedData.setFullName(
            (data.getFirstName() != null ? data.getFirstName() : "") + " " +
            (data.getLastName() != null ? data.getLastName() : "")
        );
        
        // Calculate dependent allowance (e.g., $2000 per dependent)
        double dependentAllowance = (data.getDependents() != null ? data.getDependents() : 0) * 2000.0;
        processedData.setDependentAllowance(dependentAllowance);
        
        // Calculate total deductions (tax + dependent allowance)
        double totalDeductions = (data.getSalary() != null ? data.getSalary() : 0.0) * 
                               (data.getTaxRate() != null ? data.getTaxRate() : 0.0) - 
                               dependentAllowance;
        processedData.setTotalDeductions(Math.max(0, totalDeductions)); // Ensure non-negative
        
        return processedData;
    }
}
