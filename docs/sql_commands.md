# SQL Commands Documentation

## Table Creation

```sql
CREATE TABLE sales_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id TEXT NOT NULL,
    product_name TEXT NOT NULL,
    price REAL NOT NULL,
    quantity INTEGER NOT NULL,
    sale_date TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    store_id TEXT NOT NULL,
    total_amount REAL NOT NULL
);
```

## Data Queries

### Select All Records
```sql
SELECT * FROM sales_records;
```

### Select Specific Fields
```sql
SELECT product_id, product_name, price, quantity, total_amount 
FROM sales_records;
```

### Filter by Date Range
```sql
SELECT * FROM sales_records 
WHERE sale_date BETWEEN '2024-01-01T00:00:00' AND '2024-12-31T23:59:59';
```

### Group by Product
```sql
SELECT 
    product_id,
    product_name,
    SUM(quantity) AS total_quantity,
    SUM(total_amount) AS total_revenue
FROM sales_records
GROUP BY product_id, product_name;
```

### Top Selling Products
```sql
SELECT 
    product_id,
    product_name,
    SUM(quantity) AS total_quantity
FROM sales_records
GROUP BY product_id, product_name
ORDER BY total_quantity DESC
LIMIT 5;
```

### Sales by Store
```sql
SELECT 
    store_id,
    COUNT(*) AS total_sales,
    SUM(total_amount) AS total_revenue
FROM sales_records
GROUP BY store_id;
```

### Customer Purchase History
```sql
SELECT 
    customer_id,
    COUNT(*) AS total_purchases,
    SUM(total_amount) AS total_spent
FROM sales_records
GROUP BY customer_id;
```

## Data Maintenance

### Delete All Records
```sql
DELETE FROM sales_records;
```

### Delete Specific Records
```sql
DELETE FROM sales_records 
WHERE sale_date < '2024-01-01T00:00:00';
```

### Update Record
```sql
UPDATE sales_records 
SET price = 1099.99 
WHERE product_id = 'P001';
```

## Data Export

### Export to CSV
```sql
.headers on
.mode csv
.output sales_export.csv
SELECT * FROM sales_records;
```

Note: The export command is specific to SQLite and should be run from the SQLite command-line interface. 