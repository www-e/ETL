-- Drop tables if they exist to ensure clean schema
DROP TABLE IF EXISTS processed_data;

-- Create table for processed data
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
    salary REAL,
    dependents INTEGER,
    
    -- Calculated fields
    age INTEGER,
    tax_rate REAL,
    net_salary REAL,
    full_name TEXT,
    dependent_allowance REAL,
    total_deductions REAL,
    
    -- Metadata
    processed_at TEXT,
    processing_status TEXT,
    validation_messages TEXT
);
