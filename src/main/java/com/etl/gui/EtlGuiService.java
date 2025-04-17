package com.etl.gui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.etl.model.SalesRecord;

@Service
public class EtlGuiService {
    private final JdbcTemplate jdbcTemplate;
    private String currentCsvPath;

    @Autowired
    public EtlGuiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateCsvSource(String csvPath) throws IOException {
        this.currentCsvPath = csvPath;
        jdbcTemplate.update("UPDATE batch_job_execution_context SET short_context = ? WHERE job_execution_id = ?",
            csvPath, 1);
    }

    public String getCurrentCsvPath() {
        return currentCsvPath;
    }

    public List<SalesRecord> getAllSalesRecords() {
        return jdbcTemplate.query("SELECT * FROM sales_records", new SalesRecordRowMapper());
    }

    public void exportToCsv(List<SalesRecord> records, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new File(outputPath))) {
            // Write header
            writer.println("productId,productName,price,quantity,saleDate,customerId,storeId");
            
            // Write data
            for (SalesRecord record : records) {
                writer.println(String.format("%s,%s,%.2f,%d,%s,%s,%s",
                    record.getProductId(),
                    record.getProductName(),
                    record.getPrice(),
                    record.getQuantity(),
                    record.getSaleDate().format(DateTimeFormatter.ISO_DATE_TIME),
                    record.getCustomerId(),
                    record.getStoreId()
                ));
            }
        }
    }

    public List<Map<String, Object>> getDatabaseContents(String dbPath) {
        try {
            // Create a temporary connection to the selected database
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setUrl("jdbc:sqlite:" + dbPath);
            
            JdbcTemplate tempJdbcTemplate = new JdbcTemplate(dataSource);

            // Debug log
            System.out.println("Connecting to database: " + dbPath);

            // Get all tables
            List<String> tables = tempJdbcTemplate.queryForList(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'BATCH_%'", 
                String.class
            );

            System.out.println("Found tables: " + tables);
            List<Map<String, Object>> result = new ArrayList<>();
            
            // For each table, get its contents
            for (String table : tables) {
                Map<String, Object> tableData = new HashMap<>();
                tableData.put("table", table);
                
                // Get table schema
                List<Map<String, Object>> schema = tempJdbcTemplate.queryForList(
                    "PRAGMA table_info(" + table + ")"
                );
                System.out.println("Schema for table " + table + ": " + schema);
                
                // Format schema information
                List<Map<String, String>> formattedSchema = new ArrayList<>();
                for (Map<String, Object> column : schema) {
                    Map<String, String> formattedColumn = new HashMap<>();
                    formattedColumn.put("name", String.valueOf(column.get("name")));
                    formattedColumn.put("type", String.valueOf(column.get("type")));
                    formattedColumn.put("notnull", String.valueOf(column.get("notnull")));
                    formattedColumn.put("pk", String.valueOf(column.get("pk")));
                    formattedSchema.add(formattedColumn);
                }
                tableData.put("schema", formattedSchema);
                
                // Get table data with proper formatting
                try {
                    List<Map<String, Object>> rows = tempJdbcTemplate.queryForList(
                        "SELECT * FROM " + table
                    );
                    System.out.println("Found " + rows.size() + " rows in table " + table);
                    if (!rows.isEmpty()) {
                        System.out.println("Sample row: " + rows.get(0));
                    }
                    
                    // Format the data for better readability
                    List<Map<String, String>> formattedRows = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        Map<String, String> formattedRow = new HashMap<>();
                        for (Map.Entry<String, Object> entry : row.entrySet()) {
                            formattedRow.put(entry.getKey(), 
                                entry.getValue() != null ? entry.getValue().toString() : "NULL");
                        }
                        formattedRows.add(formattedRow);
                    }
                    
                    tableData.put("data", formattedRows);
                } catch (Exception e) {
                    System.err.println("Error querying table " + table + ": " + e.getMessage());
                    e.printStackTrace();
                    tableData.put("data", new ArrayList<>());
                }
                
                result.add(tableData);
            }
            
            return result;
        } catch (RuntimeException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to read database: " + e.getMessage(), e);
        }
    }

    private static class SalesRecordRowMapper implements RowMapper<SalesRecord> {
        @Override
        public SalesRecord mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            SalesRecord record = new SalesRecord();
            record.setProductId(rs.getString("product_id"));
            record.setProductName(rs.getString("product_name"));
            record.setPrice(rs.getDouble("price"));
            record.setQuantity(rs.getInt("quantity"));
            String dateStr = rs.getString("sale_date");
            record.setSaleDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
            record.setCustomerId(rs.getString("customer_id"));
            record.setStoreId(rs.getString("store_id"));
            return record;
        }
    }
} 