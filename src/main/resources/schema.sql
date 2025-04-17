CREATE TABLE IF NOT EXISTS sales_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id TEXT NOT NULL,
    product_name TEXT NOT NULL,
    price REAL NOT NULL,
    quantity INTEGER NOT NULL,
    sale_date TIMESTAMP NOT NULL,
    customer_id TEXT NOT NULL,
    store_id TEXT NOT NULL,
    total_amount REAL NOT NULL
); 