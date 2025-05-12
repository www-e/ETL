package com.etl.etl_pipeline.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Generic model representing input data from various sources (CSV, Excel, JSON)
 * This is a flexible model that can be adapted based on the input data structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputData {
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
    
    // Calculated fields (will be populated during processing)
    private Integer age;
    private Double taxRate;
    private Double netSalary;
    private Double taxAmount;
    private Double bonus;
    private Double retirementContribution;
    private Double totalCompensation;
    
    // Explicit getters and setters for the new fields
    public Double getTaxAmount() {
        return taxAmount;
    }
    
    public void setTaxAmount(Double taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public Double getBonus() {
        return bonus;
    }
    
    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }
    
    public Double getRetirementContribution() {
        return retirementContribution;
    }
    
    public void setRetirementContribution(Double retirementContribution) {
        this.retirementContribution = retirementContribution;
    }
    
    public Double getTotalCompensation() {
        return totalCompensation;
    }
    
    public void setTotalCompensation(Double totalCompensation) {
        this.totalCompensation = totalCompensation;
    }
}
