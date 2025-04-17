# ETL Pipeline GUI

This is a JavaFX-based graphical user interface for the ETL Pipeline project. It provides a user-friendly way to:
- Select and update the source CSV file
- Run the ETL process
- View the processed data
- Export data to CSV

## Features

1. **File Selection**
   - Browse and select CSV files
   - Update the source data file
   - Validate CSV format

2. **ETL Process Control**
   - Start/Stop the ETL process
   - View process status and logs
   - Monitor progress

3. **Data Visualization**
   - Display data in a sortable table
   - Filter and search data
   - View detailed record information

4. **Data Export**
   - Export processed data to CSV
   - Choose export location
   - Customize export format

## Running the GUI

1. **Prerequisites**
   - Java 21 or later
   - Maven 3.8 or later
   - JavaFX 21 or later

2. **Build and Run**
   ```bash
   mvn clean javafx:run
   ```

3. **Using the GUI**

   a. **Selecting a CSV File**
   - Click the "Select CSV File" button
   - Browse to your CSV file
   - The file will be copied to the project's resources directory

   b. **Running the ETL Process**
   - Click the "Run ETL Test" button
   - Monitor the progress in the log area
   - View the processed data in the table

   c. **Exporting Data**
   - Click the "Export to CSV" button
   - Choose the export location
   - The data will be exported in CSV format

## Troubleshooting

1. **JavaFX Not Found**
   - Ensure JavaFX is properly installed
   - Check the JavaFX version matches your Java version
   - Verify the JavaFX modules are in the module path

2. **CSV File Issues**
   - Ensure the CSV file follows the required format
   - Check file permissions
   - Verify the file is not open in another application

3. **Database Connection**
   - Check if the SQLite database is accessible
   - Verify database permissions
   - Ensure the database schema is correct

## Development

1. **Adding New Features**
   - Extend the `EtlGuiService` class
   - Update the GUI components
   - Add new event handlers

2. **Modifying the UI**
   - Edit the FXML files
   - Update the CSS styles
   - Add new controls

3. **Testing**
   - Run unit tests: `mvn test`
   - Run GUI tests: `mvn javafx:test`

## Dependencies

- JavaFX 21.0.1
- Spring Boot 3.4.4
- ControlsFX 11.1.2
- SQLite JDBC

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request 