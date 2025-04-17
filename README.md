# ETL Pipeline Project

This project implements an ETL (Extract, Transform, Load) pipeline using Spring Batch to process sales data from a CSV file into a SQLite database.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/etl/
│   │       ├── config/        # Spring Batch configuration
│   │       ├── model/         # Data models
│   │       ├── processor/     # Data transformation logic
│   │       ├── reader/        # Data extraction
│   │       └── writer/        # Data loading
│   └── resources/
│       └── sales_data.csv    # Input data file
└── test/
    └── java/
        └── com/etl/
            └── EtlPipelineTest.java  # Integration tests
```

## Features

- CSV file reading with header row skipping
- Data transformation and validation
- SQLite database integration
- Comprehensive test coverage
- Data export capabilities

## Running the Application

1. Build the project:
```bash
mvn clean package
```

2. Run the tests:
```bash
mvn clean test
```

3. Run the application:
```bash
mvn spring-boot:run
```

## Data Viewing

After running the ETL process, you can view the data in several ways:

1. Through the test output (shows formatted table)
2. Using SQLite command line:
```bash
sqlite3 target/test-classes/test.db
SELECT * FROM sales_records;
```

3. Export to CSV:
```bash
sqlite3 target/test-classes/test.db
.headers on
.mode csv
.output sales_export.csv
SELECT * FROM sales_records;
```

## SQL Documentation

For detailed SQL commands and examples, see [SQL Commands Documentation](docs/sql_commands.md).

## Dependencies

- Spring Boot 3.4.4
- Spring Batch
- SQLite JDBC
- JUnit 5

## Configuration

The application uses the following configuration files:
- `application.properties`: Main application configuration
- `application-test.properties`: Test-specific configuration

## Testing

The project includes integration tests that:
1. Create the database schema
2. Run the ETL job
3. Verify the loaded data
4. Clean up after testing

## Data Model

The `SalesRecord` class represents the sales data with the following fields:
- productId
- productName
- price
- quantity
- saleDate
- customerId
- storeId
- totalAmount

## Prerequisites

- Java 21
- Maven 3.8+
- VS Code with Java extensions

## Running the Project

### Using Maven

1. Open a terminal in the project root directory
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Using VS Code

1. Open the project in VS Code
2. Install the following extensions if not already installed:
   - Extension Pack for Java
   - Spring Boot Extension Pack
3. Open the Command Palette (Ctrl+Shift+P)
4. Type "Spring Boot: Run" and select the project

## Testing

Run the tests using:
```bash
mvn test
```

## Data Schema

The project uses SQLite with the following schema:

```sql
CREATE TABLE sales_records (
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
```

## Adding New Data

1. Create a new CSV file in `src/main/resources/` with the following format:
   ```
   productId,productName,price,quantity,saleDate,customerId,storeId
   P001,Product Name,99.99,1,2024-01-15T10:30:00,C001,S001
   ```

2. Update the `SalesRecordReader` to point to your new file

## Scaling the Project

To scale the project:

1. Add new tables in `schema.sql`
2. Create corresponding model classes
3. Implement new readers, processors, and writers
4. Add new job configurations in `EtlJobConfig`

## Troubleshooting

Common issues and solutions:

1. Database connection issues:
   - Check if `etl_database.db` exists
   - Verify SQLite JDBC driver is in classpath

2. CSV file not found:
   - Ensure file is in `src/main/resources/`
   - Check file permissions

3. Spring Batch errors:
   - Check application.properties configuration
   - Verify job parameters