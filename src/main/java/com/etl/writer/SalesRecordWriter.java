package com.etl.writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.etl.model.SalesRecord;

@Component
public class SalesRecordWriter implements ItemWriter<SalesRecord> {

    private final JdbcTemplate jdbcTemplate;

    public SalesRecordWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(@NonNull Chunk<? extends SalesRecord> chunk) throws Exception {
        for (SalesRecord item : chunk) {
            jdbcTemplate.update(
                "INSERT INTO sales_records (product_id, product_name, price, quantity, sale_date, customer_id, store_id, total_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSaleDate(),
                item.getCustomerId(),
                item.getStoreId(),
                item.getTotalAmount()
            );
        }
    }
} 