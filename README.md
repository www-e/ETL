# ETL Pipeline with Spring Batch

## ETL Pipeline Project

## Overview

This is a comprehensive ETL (Extract, Transform, Load) pipeline project built with Java 17, Spring Boot, and Spring Batch. The application provides a modern web interface for uploading, processing, and visualizing data through a complete ETL workflow.

## Features

### Backend
- **Multi-format Data Processing**: Support for CSV, Excel, and JSON input files
- **Advanced Data Transformation**: Cleaning, validation, and mathematical calculations
- **Multi-threaded Processing**: Parallel execution for improved performance
- **SQLite Database Storage**: Persistent storage of processed data
- **RESTful API**: Comprehensive API for frontend integration
- **Modular Architecture**: Well-organized code structure following SOLID principles

### Frontend
- **Modern UI**: Clean, responsive interface with blue-themed design
- **File Upload**: Drag-and-drop file upload with preview functionality
- **Data Visualization**: Interactive charts for analyzing processed data
- **Real-time Job Status**: Live updates on ETL job progress
- **Detailed Results View**: Comprehensive view of processed data with filtering options

## Technical Stack

### Backend
- **Language**: Java 17
- **Build Tool**: Maven
- **Framework**: Spring Boot, Spring Batch
- **Database**: SQLite
- **Libraries**: Apache POI (Excel), Jackson (JSON), Apache Commons CSV

### Frontend
- **Languages**: HTML5, CSS3, JavaScript
- **Visualization**: Chart.js
- **Icons**: Font Awesome
- **Styling**: Custom CSS with responsive design

## Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/com/etl/etl_pipeline/
│   │   │   ├── config/          # Spring and batch configuration
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── model/           # Data models and DTOs
│   │   │   ├── processor/       # Data transformation logic
│   │   │   ├── reader/          # File readers for different formats
│   │   │   ├── service/         # Business logic services
│   │   │   ├── util/            # Utility classes
│   │   │   ├── writer/          # Database writers
│   │   │   └── EtlPipelineApplication.java  # Main application class
│   │   ├── resources/
│   │   │   ├── static/
│   │   │   │   ├── css/         # Stylesheet files
│   │   │   │   ├── js/          # JavaScript files
│   │   │   │   │   ├── components/  # UI components
│   │   │   │   │   ├── services/    # API services
│   │   │   │   │   └── utils/       # Utility functions
│   │   │   ├── templates/       # HTML templates
│   │   │   ├── application.properties  # Application configuration
│   │   │   └── schema.sql       # Database schema
│   └── test/                    # Test classes
├── uploads/                     # Upload directory for input files
├── pom.xml                      # Maven dependencies
└── README.md                    # Project documentation
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Web browser (Chrome, Firefox, Edge recommended)

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application using Maven:

```bash
./mvnw spring-boot:run
```

4. Open your browser and navigate to `http://localhost:8080`

### Using the Application

1. **Upload Data**:
   - Navigate to the "Input & Preview" tab
   - Drag and drop a CSV, Excel, or JSON file, or click "Browse Files"
   - Click "Preview Data" to see the raw input data

2. **Process Data**:
   - Click "Apply ETL" to start the ETL process
   - Monitor the job status in the modal dialog

3. **View Results**:
   - Once processing is complete, click "View Results" or navigate to the "ETL Results" tab
   - Explore the processed data in the table
   - Use the chart controls to visualize different metrics
   - Click on any row to view detailed record information

## Sample Data

The project includes sample data files in the `uploads` directory:
- `sample_data.csv`: CSV sample with 30 records
- `sample_data.json`: JSON sample with 10 records

## Data Transformation

The ETL pipeline performs the following transformations:

1. **Data Cleaning**:
   - Trimming whitespace from string fields
   - Normalizing phone numbers
   - Setting default values for null fields

2. **Validation**:
   - Email format validation
   - Date format validation
   - Required field checks

3. **Mathematical Processing**:
   - Age calculation from birth date
   - Tax rate calculation based on salary
   - Net salary calculation
   - Dependent allowance calculation
   - Total deductions calculation

## API Documentation

### Endpoints

- `POST /api/etl/upload`: Upload and process a file
- `POST /api/etl/preview`: Preview file contents
- `GET /api/etl/status/{jobId}`: Get job status
- `GET /api/etl/data`: Get all processed data
- `GET /api/etl/stats`: Get statistics about processed data

## Database Schema

The application uses a SQLite database with the following schema:

```sql
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
```

## Configuration

The application can be configured through the `application.properties` file:

```properties
# ETL Configuration
etl.upload-dir=uploads      # Directory for uploaded files
etl.chunk-size=100          # Batch processing chunk size
etl.max-threads=4           # Maximum number of processing threads
```

## Future Enhancements

- Support for additional input formats (XML, Parquet, etc.)
- Advanced filtering and searching in the results view
- Export functionality for processed data
- User authentication and role-based access control
- Support for scheduled/automated ETL jobs

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Educational Objectives
- Learn ETL (Extract, Transform, Load) concepts and implementation
- Understand Spring Batch framework architecture
- Apply multi-threaded processing techniques
- Practice data transformation and validation
- Implement error handling in batch processing

## Technology Stack
- Java 21
- Spring Boot 3.4.3
- Spring Batch
- Maven
- SQLite 3
- Spring JDBC

## Prerequisites
- JDK 21
- Maven
- SQLite 3
- IDE (e.g., Visual Studio Code with Java extensions)

## Setting Up the Development Environment

### 1. Installing Java 21
1. Download JDK 21 from [Oracle's website](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) or use OpenJDK
2. Run the installer and follow the instructions
3. Set the JAVA_HOME environment variable:
   - **Windows**: 
     ```
     setx JAVA_HOME "C:\Program Files\Java\jdk-21"
     setx PATH "%PATH%;%JAVA_HOME%\bin"
     ```
4. Verify installation:
   ```
   java -version
   ```

### 2. Installing Maven
1. Download Maven from [Apache Maven website](https://maven.apache.org/download.cgi)
2. Extract the archive to a directory of your choice
3. Set the environment variables:
   - **Windows**:
     ```
     setx M2_HOME "C:\path\to\maven"
     setx PATH "%PATH%;%M2_HOME%\bin"
     ```
4. Verify installation:
   ```
   mvn -version
   ```

### 3. Installing SQLite
1. Download SQLite from [SQLite website](https://www.sqlite.org/download.html)
2. For Windows, download the precompiled binaries
3. Extract the files to a folder (e.g., C:\sqlite)
4. Add the directory to your PATH:
   ```
   setx PATH "%PATH%;C:\sqlite"
   ```
5. Verify installation:
   ```
   sqlite3 --version
   ```

### 4. Setting Up Visual Studio Code
1. Download and install VS Code from [Visual Studio Code website](https://code.visualstudio.com/)
2. Install the following extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - SQLite
   - Maven for Java

### 5. Creating a Project with Spring Initializer
1. Go to [Spring Initializer](https://start.spring.io/)
2. Configure the project:
   - Project: Maven
   - Language: Java
   - Spring Boot: 3.4.3
   - Group: com.etl
   - Artifact: etl-pipeline
   - Name: etl-pipeline
   - Description: ETL Pipeline with Spring Batch
   - Package name: com.etl
   - Packaging: Jar
   - Java: 21
3. Add the following dependencies:
   - Spring Batch
   - Spring Web
   - JDBC API
   - Spring Boot DevTools (optional)
4. Generate the project and download the ZIP file
5. Extract the ZIP file to your desired location

## Getting Started with Git

### Cloning the Repository
```bash
git clone https://github.com/www-e/ETL.git
cd etl-pipeline
```

### Creating and Switching to a New Branch
```bash
# Create a new branch
git branch feature/your-feature-name

# Switch to your new branch
git checkout feature/your-feature-name

# Or create and switch in one command
git checkout -b feature/your-feature-name
```

### Adding SQLite Dependency
After setting up the project, add the SQLite dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
    <scope>runtime</scope>
</dependency>
```

### Configuring SQLite in application.properties
Configure your `application.properties` file with the following settings:

```properties
# SQLite Configuration
spring.datasource.url=jdbc:sqlite:etl_database.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.username=
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.SQLiteDialect

# Hibernate Configuration (Optional)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Spring Batch Configuration
spring.batch.job.enabled=false
spring.batch.initialize-schema=always
```

## Building and Running the Project

### Building the Project
```bash
mvn clean install
```

### Running the Application
```bash
mvn spring-boot:run
```

## Project Structure
```
etl-pipeline/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── etl/
│   │   │           ├── config/       # Configuration classes
│   │   │           ├── model/        # Data models
│   │   │           ├── processor/    # Data processors
│   │   │           ├── reader/       # Data readers
│   │   │           ├── writer/       # Data writers
│   │   │           └── EtlPipelineApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data/
│   │           └── sales_data.csv    # Sample data
│   └── test/       # Test classes
├── pom.xml
└── README.md
```

## Dataset
This project uses a simple sales dataset in CSV format with fields like product ID, product name, quantity, price, and transaction date. This dataset will be processed and stored in a structured format in the SQLite database.

## Development Process
See the [DEVELOPMENT.md](DEVELOPMENT.md) file for detailed information on the development process and educational aspects of this project.


## Git Workflow

### Committing Changes
```bash
# Add your changes
git add .

# Commit with a meaningful message
git commit -m "Description of changes made"
```

### Pushing to Remote
```bash
# Push to your branch
git push origin feature/your-feature-name
```

### Creating a Pull Request
1. Navigate to the repository on GitHub
2. Click on "Pull Requests" tab
3. Click on "New Pull Request"
4. Select your branch as the compare branch
5. Click "Create Pull Request"
6. Add a description and submit

## Course Information
- Course: [Parallel Programming]
- Professor: [Ahmed Maher]
- Team Members: [Omar Ashraf, Mohammed Adel, Esraa Adel, Nada Salah, Amira Fawzy]