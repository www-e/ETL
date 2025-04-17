package com.etl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.etl.model.SalesRecord;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public class EtlPipelineTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job etlJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @BeforeEach
    public void setup() {
        // Create the table if it doesn't exist
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sales_records (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "product_id TEXT NOT NULL, " +
            "product_name TEXT NOT NULL, " +
            "price REAL NOT NULL, " +
            "quantity INTEGER NOT NULL, " +
            "sale_date TEXT NOT NULL, " +  // Changed to TEXT for SQLite
            "customer_id TEXT NOT NULL, " +
            "store_id TEXT NOT NULL, " +
            "total_amount REAL NOT NULL" +
        ")");
    }

    @AfterEach
    public void cleanup() {
        // Clean up the test database
        jdbcTemplate.execute("DELETE FROM sales_records");
    }

    @Test
    public void testEtlPipeline() throws Exception {
        // Create unique job parameters for each test run
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        System.out.println("Job Execution Status: " + jobExecution.getStatus());
        System.out.println("Job Exit Status: " + jobExecution.getExitStatus());
        
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        // Query and display the loaded data
        List<SalesRecord> records = jdbcTemplate.query(
            "SELECT * FROM sales_records",
            (rs, rowNum) -> {
                SalesRecord record = new SalesRecord();
                record.setId(rs.getLong("id"));
                record.setProductId(rs.getString("product_id"));
                record.setProductName(rs.getString("product_name"));
                record.setPrice(rs.getDouble("price"));
                record.setQuantity(rs.getInt("quantity"));
                // Parse the timestamp string directly
                String dateStr = rs.getString("sale_date");
                record.setSaleDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                record.setCustomerId(rs.getString("customer_id"));
                record.setStoreId(rs.getString("store_id"));
                record.setTotalAmount(rs.getDouble("total_amount"));
                return record;
            }
        );

        // Display the loaded data
        System.out.println("\nLoaded Sales Records:");
        System.out.println("ID | Product ID | Product Name | Price | Quantity | Sale Date | Customer ID | Store ID | Total Amount");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (SalesRecord record : records) {
            System.out.printf("%d | %s | %s | %.2f | %d | %s | %s | %s | %.2f%n",
                record.getId(),
                record.getProductId(),
                record.getProductName(),
                record.getPrice(),
                record.getQuantity(),
                record.getSaleDate(),
                record.getCustomerId(),
                record.getStoreId(),
                record.getTotalAmount()
            );
        }

        // Verify we have the expected number of records
        assertEquals(50, records.size());

        // Verify the first record
        SalesRecord firstRecord = records.get(0);
        assertEquals("P001", firstRecord.getProductId());
        assertEquals("Laptop Pro", firstRecord.getProductName());
        assertEquals(999.99, firstRecord.getPrice());
        assertEquals(2, firstRecord.getQuantity());
        assertEquals(1999.98, firstRecord.getTotalAmount());
    }
} 