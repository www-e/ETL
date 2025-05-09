/**
 * Utility functions for formatting data
 */
const formatters = {
    /**
     * Format a number as currency
     * @param {number} value - The value to format
     * @param {string} currency - The currency symbol (default: $)
     * @returns {string} - Formatted currency string
     */
    currency: (value, currency = '$') => {
        if (value === null || value === undefined) return `${currency}0.00`;
        return `${currency}${parseFloat(value).toFixed(2).replace(/\d(?=(\d{3})+\.)/g, '$&,')}`;
    },

    /**
     * Format a number with commas
     * @param {number} value - The value to format
     * @returns {string} - Formatted number string
     */
    number: (value) => {
        if (value === null || value === undefined) return '0';
        return parseFloat(value).toLocaleString('en-US');
    },

    /**
     * Format a percentage
     * @param {number} value - The value to format (0-1)
     * @returns {string} - Formatted percentage string
     */
    percentage: (value) => {
        if (value === null || value === undefined) return '0%';
        return `${(parseFloat(value) * 100).toFixed(2)}%`;
    },

    /**
     * Format a date string
     * @param {string} dateStr - The date string to format
     * @returns {string} - Formatted date string
     */
    date: (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    },

    /**
     * Format a datetime string
     * @param {string} dateTimeStr - The datetime string to format
     * @returns {string} - Formatted datetime string
     */
    dateTime: (dateTimeStr) => {
        if (!dateTimeStr) return '';
        const date = new Date(dateTimeStr);
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    },

    /**
     * Truncate a string to a specified length
     * @param {string} str - The string to truncate
     * @param {number} length - The maximum length
     * @returns {string} - Truncated string
     */
    truncate: (str, length = 30) => {
        if (!str) return '';
        return str.length > length ? str.substring(0, length) + '...' : str;
    },

    /**
     * Format a file size
     * @param {number} bytes - The file size in bytes
     * @returns {string} - Formatted file size string
     */
    fileSize: (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
};
