# ETL Pipeline with Spring Batch

## Project Overview
This project implements an ETL (Extract, Transform, Load) pipeline using Spring Batch. The pipeline is designed to read data from CSV files, perform transformations, and load the processed data into a SQLite database. It demonstrates the fundamentals of ETL processes while leveraging the Spring Batch framework.

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

## Getting Started

### Build the Project
```bash
mvn clean install
```

### Run the Application
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
│   │       └── application.properties
│   └── test/       # Test classes
├── pom.xml
└── README.md
```

## Dataset
This project uses a simple sales dataset in CSV format with fields like product ID, product name, quantity, price, and transaction date. This dataset will be processed and stored in a structured format in the SQLite database.

## Development Process
See the [DEVELOPMENT.md](DEVELOPMENT.md) file for detailed information on the development process and educational aspects of this project.

## Course Information
- Course: [Course Name]
- Professor: [Professor Name]
- Semester: [Semester]
- Team Members: [Team Members]
