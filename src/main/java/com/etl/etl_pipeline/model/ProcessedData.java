package com.etl.etl_pipeline.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing data after ETL processing
 * Contains all the original fields plus calculated and transformed fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedData {
    // Original fields (possibly cleaned/transformed)
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private String address;
    private String city;
    private String country;
    private String phoneNumber;
    private Double salary;
    private Integer dependents;
    
    // Calculated fields
    private Integer age;
    private Double taxRate;
    private Double netSalary;
    private String fullName;
    private Double dependentAllowance;
    private Double totalDeductions;
    private Double bonus;                    // Performance or seniority bonus
    private Double retirementContribution;   // Retirement plan contribution
    private Double totalCompensation;        // Total compensation including bonus
    private Double taxAmount;                // Actual tax amount in currency
    
    // Metadata
    private LocalDateTime processedAt;
    private String processingStatus;
    private String validationMessages;
}
