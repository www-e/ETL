# Mathematical Operations in the ETL Process

## Overview

The ETL application performs various mathematical operations during the transformation phase of the ETL process. These operations are primarily implemented in the `DataProcessor` class, which is responsible for transforming raw input data into processed data. This document provides a detailed analysis of all mathematical operations performed in the ETL process.

## Tax Calculations

### Tax Rate Determination

The application calculates tax rates based on salary brackets, which are configurable through application properties:

```java
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
```

The tax brackets and rates are configurable:

```java
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
```

### Tax Amount Calculation

The application calculates the actual tax amount by multiplying the salary by the tax rate:

```java
// Calculate tax amount
double taxAmount = salary * taxRate;
data.setTaxAmount(taxAmount);
```

## Compensation Calculations

### Bonus Calculation

The application calculates a bonus based on a configurable percentage of the salary:

```java
// Calculate bonus (e.g., 5% of salary)
double bonus = salary * bonusPercentage;
data.setBonus(bonus);
```

The bonus percentage is configurable:

```java
// Configurable bonus percentage based on salary
@Value("${etl.bonus.percentage:0.05}")
private double bonusPercentage;
```

### Retirement Contribution Calculation

The application calculates retirement contributions based on a configurable percentage of the salary:

```java
// Calculate retirement contribution (e.g., 3% of salary)
double retirementContribution = salary * retirementContributionPercentage;
data.setRetirementContribution(retirementContribution);
```

The retirement contribution percentage is configurable:

```java
// Configurable retirement contribution percentage
@Value("${etl.retirement.contribution:0.03}")
private double retirementContributionPercentage;
```

### Total Compensation Calculation

The application calculates total compensation by adding the salary and bonus:

```java
// Calculate total compensation (salary + bonus)
double totalCompensation = salary + bonus;
data.setTotalCompensation(totalCompensation);
```

### Net Salary Calculation

The application calculates net salary by subtracting tax amount and retirement contribution from the salary and adding the bonus:

```java
// Calculate net salary after tax and retirement contribution
double netSalary = salary - taxAmount - retirementContribution + bonus;
data.setNetSalary(netSalary);
```

## Dependent Allowance Calculations

The application calculates dependent allowances based on the number of dependents and a configurable allowance amount per dependent:

```java
// Calculate dependent allowance based on configurable amount per dependent
double dependentAllowance = (data.getDependents() != null ? data.getDependents() : 0) * dependentAllowancePerDependent;
processedData.setDependentAllowance(dependentAllowance);
```

The dependent allowance amount is configurable:

```java
// Configurable dependent allowance amount per dependent
@Value("${etl.dependent.allowance:2000}")
private double dependentAllowancePerDependent;
```

## Deduction Calculations

The application calculates total deductions by subtracting the dependent allowance from the tax amount:

```java
// Calculate total deductions (tax amount - dependent allowance)
double totalDeductions = (data.getTaxAmount() != null ? data.getTaxAmount() : 0.0) - dependentAllowance;
processedData.setTotalDeductions(Math.max(0, totalDeductions)); // Ensure non-negative
```

## Age Calculation

The application calculates age based on birth date using a utility method:

```java
// Calculate age
if (data.getBirthDate() != null) {
    data.setAge(DateUtils.calculateAge(data.getBirthDate()));
}
```

The `DateUtils.calculateAge` method calculates the age based on the birth date and the current date:

```java
public static int calculateAge(LocalDate birthDate) {
    if (birthDate == null) {
        return 0;
    }
    
    LocalDate currentDate = LocalDate.now();
    
    int age = currentDate.getYear() - birthDate.getYear();
    
    // Adjust age if birthday hasn't occurred yet this year
    if (currentDate.getMonthValue() < birthDate.getMonthValue() || 
        (currentDate.getMonthValue() == birthDate.getMonthValue() && 
         currentDate.getDayOfMonth() < birthDate.getDayOfMonth())) {
        age--;
    }
    
    return Math.max(0, age); // Ensure non-negative
}
```

## Statistical Calculations

The application calculates various statistics about the processed data:

```java
public Map<String, Object> getStatistics() {
    Map<String, Object> stats = new HashMap<>();
    
    try {
        // Count total records
        Integer totalRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM processed_data", Integer.class);
        stats.put("totalRecords", totalRecords != null ? totalRecords : 0);
        
        // Calculate average salary
        Double avgSalary = jdbcTemplate.queryForObject(
            "SELECT AVG(salary) FROM processed_data", Double.class);
        stats.put("averageSalary", avgSalary != null ? avgSalary : 0.0);
        
        // Calculate average age
        Double avgAge = jdbcTemplate.queryForObject(
            "SELECT AVG(age) FROM processed_data", Double.class);
        stats.put("averageAge", avgAge != null ? avgAge : 0.0);
        
        // Calculate total salary
        Double totalSalary = jdbcTemplate.queryForObject(
            "SELECT SUM(salary) FROM processed_data", Double.class);
        stats.put("totalSalary", totalSalary != null ? totalSalary : 0.0);
        
        // Count records by country
        List<Map<String, Object>> countryStats = jdbcTemplate.queryForList(
            "SELECT country, COUNT(*) as count FROM processed_data GROUP BY country ORDER BY count DESC");
        stats.put("recordsByCountry", countryStats);
        
        // Count records by processing status
        List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(
            "SELECT processing_status, COUNT(*) as count FROM processed_data GROUP BY processing_status");
        stats.put("recordsByStatus", statusStats);
        
        // Calculate min and max salary
        Double minSalary = jdbcTemplate.queryForObject(
            "SELECT MIN(salary) FROM processed_data", Double.class);
        Double maxSalary = jdbcTemplate.queryForObject(
            "SELECT MAX(salary) FROM processed_data", Double.class);
        stats.put("minSalary", minSalary != null ? minSalary : 0.0);
        stats.put("maxSalary", maxSalary != null ? maxSalary : 0.0);
        
    } catch (Exception e) {
        log.error("Error calculating statistics", e);
        stats.put("error", "Failed to calculate statistics: " + e.getMessage());
    }
    
    return stats;
}
```

These statistics include:
- Total number of records
- Average salary
- Average age
- Total salary
- Records by country
- Records by processing status
- Minimum and maximum salary

## Retry Backoff Calculations

The application uses an exponential backoff algorithm for retries:

```java
private long calculateBackoffDelay(int attempt) {
    // Calculate exponential backoff with jitter
    double exponentialFactor = Math.pow(backoffMultiplier, attempt - 1);
    long delay = (long) (initialRetryDelayMs * exponentialFactor);
    
    // Add some randomness (jitter) to prevent synchronized retries
    // Use a percentage of the base delay for jitter (20%)
    delay += (long) (delay * 0.2 * Math.random());
    
    // Cap at max delay
    return Math.min(delay, maxRetryDelayMs);
}
```

This algorithm:
1. Calculates an exponential factor based on the attempt number and a multiplier
2. Multiplies the initial delay by this factor
3. Adds random jitter to prevent synchronized retries
4. Caps the delay at a maximum value

## Batch Size Optimization

The application calculates optimal batch sizes based on the total number of items:

```java
private int calculateOptimalBatchSize(int totalItems) {
    // For very small batches, just process them all at once
    if (totalItems <= 10) {
        return totalItems;
    }
    
    // For larger batches, use a smaller size to reduce lock contention
    // The larger the batch, the smaller the chunk size (proportionally)
    if (totalItems <= 50) {
        return 10;
    } else if (totalItems <= 100) {
        return 20;
    } else if (totalItems <= 500) {
        return 25;
    } else if (totalItems <= 1000) {
        return 50;
    } else {
        return 100; // Cap at 100 for very large batches
    }
}
```

This algorithm adjusts the batch size based on the total number of items to optimize performance and reduce lock contention.

## Summary of Mathematical Operations

The ETL application performs the following mathematical operations:

1. **Tax Calculations**:
   - Tax rate determination based on salary brackets
   - Tax amount calculation (salary × tax rate)

2. **Compensation Calculations**:
   - Bonus calculation (salary × bonus percentage)
   - Retirement contribution calculation (salary × retirement contribution percentage)
   - Total compensation calculation (salary + bonus)
   - Net salary calculation (salary - tax amount - retirement contribution + bonus)

3. **Dependent Allowance Calculations**:
   - Dependent allowance calculation (number of dependents × allowance per dependent)

4. **Deduction Calculations**:
   - Total deductions calculation (tax amount - dependent allowance)

5. **Age Calculation**:
   - Age calculation based on birth date and current date

6. **Statistical Calculations**:
   - Average salary calculation
   - Average age calculation
   - Total salary calculation
   - Minimum and maximum salary calculation
   - Count of records by country and processing status

7. **System Optimization Calculations**:
   - Exponential backoff calculation for retries
   - Optimal batch size calculation based on total items

## Configurability of Mathematical Operations

Many of the mathematical operations in the ETL application are configurable through application properties:

```
# Tax brackets
etl.tax.bracket1=20000
etl.tax.bracket2=50000
etl.tax.bracket3=100000

# Tax rates
etl.tax.rate1=0.10
etl.tax.rate2=0.15
etl.tax.rate3=0.20
etl.tax.rate4=0.25

# Dependent allowance
etl.dependent.allowance=2000

# Bonus percentage
etl.bonus.percentage=0.05

# Retirement contribution percentage
etl.retirement.contribution=0.03
```

This configurability allows for easy adjustment of the mathematical operations without changing the code.

## Conclusion

The ETL application performs a wide range of mathematical operations during the transformation phase of the ETL process. These operations are primarily focused on financial calculations such as tax, compensation, and deductions, as well as system optimization calculations for performance tuning. The configurable nature of these operations allows for flexibility and adaptability to different business requirements.
