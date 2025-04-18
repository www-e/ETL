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
    private static final String INSERT_SQL = 
        "INSERT INTO sales_records (product_id, product_name, price, quantity, sale_date, customer_id, store_id, total_amount) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    public SalesRecordWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(@NonNull Chunk<? extends SalesRecord> chunk) throws Exception {
        jdbcTemplate.batchUpdate(
            INSERT_SQL,
            chunk.getItems(),
            chunk.size(),
            (ps, item) -> {
                ps.setString(1, item.getProductId());
                ps.setString(2, item.getProductName());
                ps.setDouble(3, item.getPrice());
                ps.setInt(4, item.getQuantity());
                ps.setObject(5, item.getSaleDate());
                ps.setString(6, item.getCustomerId());
                ps.setString(7, item.getStoreId());
                ps.setDouble(8, item.getTotalAmount());
            }
        );
    }
} 