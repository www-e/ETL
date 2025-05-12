package com.etl.etl_pipeline.processor;

import com.etl.etl_pipeline.model.InputData;
import com.etl.etl_pipeline.model.ProcessedData;
import com.etl.etl_pipeline.util.DateUtils;
import com.etl.etl_pipeline.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
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
    
    // Configurable tax brackets
    @Value("${etl.tax.bracket1:20000}")
    private double taxBracket1;
    
    @Value("${etl.tax.bracket2:50000}")
    private double taxBracket2;
    
    @Value("${etl.tax.bracket3:100000}")
    private double taxBracket3;
    
    // Configurable tax rates
    @Value("${etl.tax.rate1:0.10}")
    private double taxRate1;
    
    @Value("${etl.tax.rate2:0.15}")
    private double taxRate2;
    
    @Value("${etl.tax.rate3:0.20}")
    private double taxRate3;
    
    @Value("${etl.tax.rate4:0.25}")
    private double taxRate4;
    
    // Configurable dependent allowance amount per dependent
    @Value("${etl.dependent.allowance:2000}")
    private double dependentAllowancePerDependent;
    
    // Configurable bonus percentage based on salary
    @Value("${etl.bonus.percentage:0.05}")
    private double bonusPercentage;
    
    // Configurable retirement contribution percentage
    @Value("${etl.retirement.contribution:0.03}")
    private double retirementContributionPercentage;

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
        
        // Calculate tax rate based on salary using configurable tax brackets
        double salary = data.getSalary() != null ? data.getSalary() : 0.0;
        double taxRate;
        
        if (salary <= taxBracket1) {
            taxRate = taxRate1;
        } else if (salary <= taxBracket2) {
            taxRate = taxRate2;
        } else if (salary <= taxBracket3) {
            taxRate = taxRate3;
        } else {
            taxRate = taxRate4;
        }
        
        data.setTaxRate(taxRate);
        
        // Calculate tax amount
        double taxAmount = salary * taxRate;
        
        // Calculate bonus (e.g., 5% of salary)
        double bonus = salary * bonusPercentage;
        
        // Calculate retirement contribution (e.g., 3% of salary)
        double retirementContribution = salary * retirementContributionPercentage;
        
        // Calculate total compensation (salary + bonus)
        double totalCompensation = salary + bonus;
        
        // Store calculated values
        data.setTaxAmount(taxAmount);
        data.setBonus(bonus);
        data.setRetirementContribution(retirementContribution);
        data.setTotalCompensation(totalCompensation);
        
        // Calculate net salary after tax and retirement contribution
        double netSalary = salary - taxAmount - retirementContribution + bonus;
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
            .bonus(data.getBonus())
            .retirementContribution(data.getRetirementContribution())
            .totalCompensation(data.getTotalCompensation())
            .taxAmount(data.getTaxAmount())
            .build();
        
        // Calculate additional fields
        processedData.setFullName(
            (data.getFirstName() != null ? data.getFirstName() : "") + " " +
            (data.getLastName() != null ? data.getLastName() : "")
        );
        
        // Calculate dependent allowance based on configurable amount per dependent
        double dependentAllowance = (data.getDependents() != null ? data.getDependents() : 0) * dependentAllowancePerDependent;
        processedData.setDependentAllowance(dependentAllowance);
        
        // Calculate total deductions (tax amount - dependent allowance)
        double totalDeductions = (data.getTaxAmount() != null ? data.getTaxAmount() : 0.0) - dependentAllowance;
        processedData.setTotalDeductions(Math.max(0, totalDeductions)); // Ensure non-negative
        
        return processedData;
    }
}
