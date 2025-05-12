-- SQLite-compatible schema update script

-- Create a temporary table with all required columns
CREATE TABLE IF NOT EXISTS temp_processed_data (
    id TEXT PRIMARY KEY,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    birth_date TEXT,
    address TEXT,
    city TEXT,
    country TEXT,
    phone_number TEXT,
    salary DOUBLE,
    dependents INTEGER,
    age INTEGER,
    tax_rate DOUBLE,
    net_salary DOUBLE,
    full_name TEXT,
    dependent_allowance DOUBLE,
    total_deductions DOUBLE,
    bonus DOUBLE,
    retirement_contribution DOUBLE,
    total_compensation DOUBLE,
    tax_amount DOUBLE,
    processed_at TEXT,
    processing_status TEXT,
    validation_messages TEXT
);

-- Check if processed_data table exists
SELECT name FROM sqlite_master WHERE type='table' AND name='processed_data';

-- Create processed_data table if it doesn't exist
CREATE TABLE IF NOT EXISTS processed_data (
    id TEXT PRIMARY KEY,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    birth_date TEXT,
    address TEXT,
    city TEXT,
    country TEXT,
    phone_number TEXT,
    salary DOUBLE,
    dependents INTEGER,
    age INTEGER,
    tax_rate DOUBLE,
    net_salary DOUBLE,
    full_name TEXT,
    dependent_allowance DOUBLE,
    total_deductions DOUBLE,
    bonus DOUBLE,
    retirement_contribution DOUBLE,
    total_compensation DOUBLE,
    tax_amount DOUBLE,
    processed_at TEXT,
    processing_status TEXT,
    validation_messages TEXT
);

-- Drop the temporary table
DROP TABLE IF EXISTS temp_processed_data;
