package com.etl.etl_pipeline.util;

import java.util.regex.Pattern;

/**
 * Utility class for data validation
 */
public class ValidationUtils {

    // Regular expression for validating email addresses
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    // Regular expression for validating phone numbers (basic)
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?[0-9\\s\\-()]{8,20}$");

    /**
     * Validates if a string is a valid email address
     * @param email Email to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates if a string is a valid phone number
     * @param phone Phone number to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Checks if a string is null or empty
     * @param str String to check
     * @return True if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validates if a number is positive
     * @param value Number to validate
     * @return True if positive, false otherwise
     */
    public static boolean isPositive(Number value) {
        if (value == null) {
            return false;
        }
        
        return value.doubleValue() > 0;
    }

    /**
     * Validates if a number is non-negative
     * @param value Number to validate
     * @return True if non-negative, false otherwise
     */
    public static boolean isNonNegative(Number value) {
        if (value == null) {
            return false;
        }
        
        return value.doubleValue() >= 0;
    }
}
