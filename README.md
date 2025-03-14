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