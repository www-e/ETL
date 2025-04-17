package com.etl.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import com.etl.model.SalesRecord;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.etl.config.EtlJobConfig;
import com.etl.reader.SalesRecordReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.springframework.batch.item.ItemReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

@SpringBootApplication
@Import(EtlJobConfig.class)
public class EtlGuiApplication extends Application {
    private TableView<SalesRecord> tableView;
    private ObservableList<SalesRecord> data;
    private TextArea logArea;
    private JobLauncher jobLauncher;
    private Job etlJob;
    private EtlGuiService etlService;
    private static ConfigurableApplicationContext context;
    private static final String DATA_DIR = "data";
    private static final String CSV_DIR = DATA_DIR + "/csv";
    private static final String DB_DIR = DATA_DIR + "/db";
    private TabPane mainTabPane;

    @Override
    public void init() throws Exception {
        // Create data directories if they don't exist
        createDataDirectories();
        
        // Copy default CSV file from resources to data/csv directory
        copyDefaultCsvFile();
        
        // Initialize Spring context
        context = SpringApplication.run(EtlGuiApplication.class);
        jobLauncher = context.getBean(JobLauncher.class);
        etlJob = context.getBean(Job.class);
        etlService = context.getBean(EtlGuiService.class);
    }

    private void createDataDirectories() throws IOException {
        Files.createDirectories(Paths.get(CSV_DIR));
        Files.createDirectories(Paths.get(DB_DIR));
    }

    private void copyDefaultCsvFile() throws IOException {
        Path sourcePath = Paths.get("src/main/resources/sales_data.csv");
        Path targetPath = Paths.get(CSV_DIR, "sales_data.csv");
        
        if (Files.exists(sourcePath) && !Files.exists(targetPath)) {
            Files.copy(sourcePath, targetPath);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ETL Pipeline GUI");

        // Create main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Create tab pane
        mainTabPane = new TabPane();
        
        // Create main tab
        Tab mainTab = new Tab("ETL Operations");
        mainTab.setClosable(false);
        mainTab.setContent(createMainContent());
        
        // Create CSV files tab
        Tab csvTab = new Tab("CSV Files");
        csvTab.setClosable(false);
        csvTab.setContent(createCsvFilesContent());
        
        // Create DB files tab
        Tab dbTab = new Tab("Database Files");
        dbTab.setClosable(false);
        dbTab.setContent(createDbFilesContent());

        mainTabPane.getTabs().addAll(mainTab, csvTab, dbTab);
        mainLayout.setCenter(mainTabPane);

        // Create log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(100);
        mainLayout.setBottom(logArea);

        // Set up the scene
        Scene scene = new Scene(mainLayout, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createMainContent() {
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(10));

        // Create top controls
        HBox topControls = new HBox(10);
        Button selectFileButton = new Button("Select CSV File");
        Button runTestButton = new Button("Run ETL Test");
        Button exportButton = new Button("Export to CSV");
        topControls.getChildren().addAll(selectFileButton, runTestButton, exportButton);

        // Create table view
        tableView = new TableView<>();
        setupTableView();

        mainContent.getChildren().addAll(topControls, tableView);

        // Set up event handlers
        selectFileButton.setOnAction(e -> selectFile());
        runTestButton.setOnAction(e -> runEtlTest());
        exportButton.setOnAction(e -> exportToCsv());

        return mainContent;
    }

    private VBox createCsvFilesContent() {
        VBox csvContent = new VBox(10);
        csvContent.setPadding(new Insets(10));

        ListView<String> csvFileList = new ListView<>();
        updateCsvFileList(csvFileList);

        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(e -> updateCsvFileList(csvFileList));

        Button selectButton = new Button("Select File");
        selectButton.setOnAction(e -> {
            String selectedFile = csvFileList.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                handleCsvSelection(selectedFile);
            } else {
                showError("Error", "Please select a file from the list");
            }
        });

        // Add double-click support
        csvFileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedFile = csvFileList.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    handleCsvSelection(selectedFile);
                }
            }
        });

        HBox buttonBox = new HBox(10, refreshButton, selectButton);
        csvContent.getChildren().addAll(csvFileList, buttonBox);

        return csvContent;
    }

    private VBox createDbFilesContent() {
        VBox dbContent = new VBox(10);
        dbContent.setPadding(new Insets(10));

        ListView<String> dbFileList = new ListView<>();
        updateDbFileList(dbFileList);

        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(e -> updateDbFileList(dbFileList));

        Button viewButton = new Button("View Database");
        viewButton.setOnAction(e -> {
            String selectedFile = dbFileList.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                try {
                    viewDatabase(DB_DIR + "/" + selectedFile);
                } catch (Exception ex) {
                    showError("Error", "Failed to view database: " + ex.getMessage());
                }
            }
        });

        HBox buttonBox = new HBox(10, refreshButton, viewButton);
        dbContent.getChildren().addAll(dbFileList, buttonBox);

        return dbContent;
    }

    private void updateCsvFileList(ListView<String> listView) {
        try {
            List<String> files = Files.list(Paths.get(CSV_DIR))
                .filter(path -> path.toString().endsWith(".csv"))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
            listView.setItems(FXCollections.observableArrayList(files));
        } catch (IOException e) {
            showError("Error", "Failed to list CSV files: " + e.getMessage());
        }
    }

    private void updateDbFileList(ListView<String> listView) {
        try {
            List<String> files = Files.list(Paths.get(DB_DIR))
                .filter(path -> path.toString().endsWith(".db"))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
            listView.setItems(FXCollections.observableArrayList(files));
        } catch (IOException e) {
            showError("Error", "Failed to list database files: " + e.getMessage());
        }
    }

    private void viewDatabase(String dbPath) {
        try {
            // Create a new window to display database contents
            Stage dbStage = new Stage();
            dbStage.setTitle("Database Viewer - " + Paths.get(dbPath).getFileName());
            
            // Create main layout
            VBox root = new VBox(10);
            root.setPadding(new Insets(10));
            
            // Create tab pane for tables
            TabPane tabPane = new TabPane();
            
            // Get database contents
            List<Map<String, Object>> dbContents = etlService.getDatabaseContents(dbPath);
            
            // Debug log
            log("Database path: " + dbPath);
            log("Number of tables found: " + dbContents.size());
            
            for (Map<String, Object> tableData : dbContents) {
                String tableName = (String) tableData.get("table");
                log("Processing table: " + tableName);
                
                Tab tableTab = new Tab(tableName);
                tableTab.setClosable(false);
                
                // Create table view for data
                TableView<Map<String, String>> dataTableView = new TableView<>();
                dataTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                
                // Create columns based on schema
                @SuppressWarnings("unchecked")
                List<Map<String, String>> schema = (List<Map<String, String>>) tableData.get("schema");
                log("Number of columns in schema: " + schema.size());
                
                for (Map<String, String> column : schema) {
                    String columnName = column.get("name");
                    TableColumn<Map<String, String>, String> tableColumn = new TableColumn<>(columnName);
                    tableColumn.setCellValueFactory(rowData -> {
                        String value = rowData.getValue().get(columnName);
                        return new SimpleStringProperty(value != null ? value : "");
                    });
                    tableColumn.setPrefWidth(100);
                    dataTableView.getColumns().add(tableColumn);
                }
                
                // Add data to table view
                @SuppressWarnings("unchecked")
                List<Map<String, String>> data = (List<Map<String, String>>) tableData.get("data");
                log("Number of rows in data: " + (data != null ? data.size() : 0));
                
                if (data != null && !data.isEmpty()) {
                    log("First row data: " + data.get(0));
                }
                
                dataTableView.setItems(FXCollections.observableArrayList(data));
                
                // Create schema view
                TableView<Map<String, String>> schemaTableView = new TableView<>();
                schemaTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                
                TableColumn<Map<String, String>, String> nameCol = new TableColumn<>("Column Name");
                TableColumn<Map<String, String>, String> typeCol = new TableColumn<>("Type");
                TableColumn<Map<String, String>, String> notNullCol = new TableColumn<>("Not Null");
                TableColumn<Map<String, String>, String> pkCol = new TableColumn<>("Primary Key");
                
                nameCol.setCellValueFactory(rowData -> new SimpleStringProperty(rowData.getValue().get("name")));
                typeCol.setCellValueFactory(rowData -> new SimpleStringProperty(rowData.getValue().get("type")));
                notNullCol.setCellValueFactory(rowData -> 
                    new SimpleStringProperty(rowData.getValue().get("notnull").equals("1") ? "Yes" : "No"));
                pkCol.setCellValueFactory(rowData -> 
                    new SimpleStringProperty(rowData.getValue().get("pk").equals("1") ? "Yes" : "No"));
                
                schemaTableView.getColumns().addAll(nameCol, typeCol, notNullCol, pkCol);
                schemaTableView.setItems(FXCollections.observableArrayList(schema));
                
                // Create split pane for schema and data
                SplitPane splitPane = new SplitPane();
                splitPane.setOrientation(Orientation.VERTICAL);
                
                // Add schema and data views
                VBox schemaBox = new VBox(5);
                Label schemaLabel = new Label("Table Schema:");
                schemaLabel.setStyle("-fx-font-weight: bold");
                schemaBox.getChildren().addAll(schemaLabel, schemaTableView);
                
                VBox dataBox = new VBox(5);
                Label dataLabel = new Label("Table Data:");
                dataLabel.setStyle("-fx-font-weight: bold");
                Label rowCountLabel = new Label(String.format("Total Rows: %d", data != null ? data.size() : 0));
                rowCountLabel.setStyle("-fx-font-style: italic");
                dataBox.getChildren().addAll(dataLabel, rowCountLabel, dataTableView);
                
                splitPane.getItems().addAll(schemaBox, dataBox);
                splitPane.setDividerPositions(0.3);
                
                // Add copy buttons
                HBox buttonBox = new HBox(10);
                buttonBox.setPadding(new Insets(5));
                Button copySchemaBtn = new Button("Copy Schema");
                Button copyDataBtn = new Button("Copy Data");
                Button refreshBtn = new Button("Refresh Data");
                
                copySchemaBtn.setOnAction(e -> {
                    StringBuilder sb = new StringBuilder();
                    for (Map<String, String> col : schema) {
                        sb.append(String.format("%s %s %s %s\n", 
                            col.get("name"), col.get("type"),
                            col.get("notnull").equals("1") ? "NOT NULL" : "",
                            col.get("pk").equals("1") ? "PRIMARY KEY" : ""));
                    }
                    copyToClipboard(sb.toString());
                    showInfo("Success", "Schema copied to clipboard");
                });
                
                copyDataBtn.setOnAction(e -> {
                    StringBuilder sb = new StringBuilder();
                    // Add headers
                    for (Map<String, String> col : schema) {
                        sb.append(col.get("name")).append("\t");
                    }
                    sb.append("\n");
                    
                    // Add data
                    for (Map<String, String> row : data) {
                        for (Map<String, String> col : schema) {
                            sb.append(row.get(col.get("name"))).append("\t");
                        }
                        sb.append("\n");
                    }
                    
                    copyToClipboard(sb.toString());
                    showInfo("Success", "Data copied to clipboard");
                });
                
                refreshBtn.setOnAction(e -> {
                    try {
                        List<Map<String, Object>> refreshedContents = etlService.getDatabaseContents(dbPath);
                        for (Map<String, Object> refreshedTable : refreshedContents) {
                            if (tableName.equals(refreshedTable.get("table"))) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, String>> refreshedData = (List<Map<String, String>>) refreshedTable.get("data");
                                dataTableView.setItems(FXCollections.observableArrayList(refreshedData));
                                rowCountLabel.setText(String.format("Total Rows: %d", refreshedData != null ? refreshedData.size() : 0));
                                log("Data refreshed for table: " + tableName + ", Row count: " + (refreshedData != null ? refreshedData.size() : 0));
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        showError("Refresh Error", "Failed to refresh data: " + ex.getMessage());
                    }
                });
                
                buttonBox.getChildren().addAll(copySchemaBtn, copyDataBtn, refreshBtn);
                
                // Add everything to the tab
                VBox tabContent = new VBox(10);
                tabContent.getChildren().addAll(buttonBox, splitPane);
                tableTab.setContent(tabContent);
                
                tabPane.getTabs().add(tableTab);
            }
            
            root.getChildren().add(tabPane);
            
            // Create scene and show stage
            Scene scene = new Scene(root, 1000, 600);
            dbStage.setScene(scene);
            dbStage.show();
        } catch (Exception e) {
            showError("Database Error", "Failed to view database: " + e.getMessage());
            log("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
    }

    private void setupTableView() {
        @SuppressWarnings("unchecked")
        TableColumn<SalesRecord, ?>[] columns = new TableColumn[] {
            createColumn("Product ID", "productId"),
            createColumn("Product Name", "productName"),
            createColumn("Price", "price"),
            createColumn("Quantity", "quantity"),
            createColumn("Sale Date", "saleDate"),
            createColumn("Customer ID", "customerId"),
            createColumn("Store ID", "storeId")
        };
        
        tableView.getColumns().addAll(columns);
    }

    private <T> TableColumn<SalesRecord, T> createColumn(String title, String property) {
        TableColumn<SalesRecord, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            try {
                // Copy file to CSV directory
                Path targetPath = Paths.get(CSV_DIR, selectedFile.getName());
                Files.copy(selectedFile.toPath(), targetPath);
                
                etlService.updateCsvSource(targetPath.toString());
                log("CSV file updated successfully: " + selectedFile.getName());
            } catch (IOException e) {
                showError("Error", "Failed to update CSV file: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void runEtlTest() {
        String selectedFile = etlService.getCurrentCsvPath();
        if (selectedFile == null) {
            showError("Error", "Please select a CSV file first");
            return;
        }

        // Generate a unique database name with timestamp and source file name
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sourceFileName = new File(selectedFile).getName().replace(".csv", "");
        String dbName = "sales_" + sourceFileName + "_" + timestamp + ".db";
        
        // Ensure the db directory exists
        try {
            Files.createDirectories(Paths.get(DB_DIR));
        } catch (IOException e) {
            showError("Error", "Failed to create database directory: " + e.getMessage());
            return;
        }

        // Set the database path
        String dbPath = Paths.get(DB_DIR, dbName).toAbsolutePath().toString();
        System.setProperty("db.path", dbPath);

        log("Starting ETL process with file: " + selectedFile);
        log("Output database: " + dbPath);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Initialize the database
                    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                        Statement stmt = conn.createStatement();
                        stmt.execute("""
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
                            )
                        """);
                    }

                    // Get the reader and update its file path
                    SalesRecordReader reader = (SalesRecordReader) context.getBean(ItemReader.class);
                    reader.setFilePath(selectedFile);

                    // Create job parameters
                    JobParameters jobParameters = new JobParametersBuilder()
                            .addLong("time", System.currentTimeMillis())
                            .toJobParameters();

                    // Run the job
                    JobExecution jobExecution = jobLauncher.run(etlJob, jobParameters);
                    
                    // Log the results
                    updateMessage("Job Status: " + jobExecution.getStatus());
                    updateMessage("Exit Status: " + jobExecution.getExitStatus());
                    
                    if (jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
                        updateMessage("ETL process completed successfully!");
                        updateMessage("Database created: " + dbPath);
                        updateData();
                    } else {
                        updateMessage("ETL process failed!");
                    }
                } catch (Exception e) {
                    updateMessage("Error during ETL process: " + e.getMessage());
                    throw e;
                }
                return null;
            }
        };

        // Bind task messages to log area
        task.messageProperty().addListener((obs, oldVal, newVal) -> {
            logArea.appendText(newVal + "\n");
        });

        // Start the task
        new Thread(task).start();
    }

    private void updateData() {
        List<SalesRecord> records = etlService.getAllSalesRecords();
        data = FXCollections.observableArrayList(records);
        tableView.setItems(data);
    }

    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialDirectory(new File(CSV_DIR));
        File selectedFile = fileChooser.showSaveDialog(null);
        
        if (selectedFile != null) {
            try {
                etlService.exportToCsv(data, selectedFile.getAbsolutePath());
                log("Data exported successfully to: " + selectedFile.getName());
            } catch (IOException e) {
                showError("Error", "Failed to export data: " + e.getMessage());
            }
        }
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleCsvSelection(String selectedFile) {
        try {
            String fullPath = CSV_DIR + "/" + selectedFile;
            etlService.updateCsvSource(fullPath);
            log("CSV file selected: " + selectedFile);
            
            // Switch to ETL Operations tab
            mainTabPane.getSelectionModel().select(0);
            
            // Show confirmation dialog
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Run ETL Process");
            alert.setHeaderText("CSV File Selected: " + selectedFile);
            alert.setContentText("Do you want to run the ETL process now?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    runEtlTest();
                }
            });
        } catch (IOException ex) {
            showError("Error", "Failed to select file: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 