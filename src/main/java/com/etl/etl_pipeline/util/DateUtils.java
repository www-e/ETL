package com.etl.etl_pipeline.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for date operations
 */
public class DateUtils {

    // Common date formats to try when parsing
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ISO_DATE,                    // 2023-01-31
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),     // 01/31/2023
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),     // 31/01/2023
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),     // 01-31-2023
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),     // 2023/01/31
        DateTimeFormatter.ofPattern("dd-MMM-yyyy"),    // 31-Jan-2023
        DateTimeFormatter.ofPattern("MMM dd, yyyy")    // Jan 31, 2023
    );

    /**
     * Parses a date string using multiple common formats
     * @param dateStr Date string to parse
     * @return LocalDate object or null if parsing fails
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        dateStr = dateStr.trim();
        
        // Try each format until one succeeds
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        return null;
    }

    /**
     * Calculates age based on birth date
     * @param birthDate Birth date
     * @return Age in years
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Formats a date to ISO format (yyyy-MM-dd)
     * @param date Date to format
     * @return Formatted date string or empty string if date is null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        
        return date.format(DateTimeFormatter.ISO_DATE);
    }
}
