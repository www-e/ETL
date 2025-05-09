package com.etl.etl_pipeline.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.lang.NonNull;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Batch Configuration to handle SQLite-specific configurations for Spring Batch.
 * This class customizes the transaction management to prevent SQLite locking issues.
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class CustomBatchConfigurer extends DefaultBatchConfiguration {

    private final DataSource dataSource;

    @Autowired
    public CustomBatchConfigurer(DataSource dataSource) {
        this.dataSource = dataSource;
        log.info("Initializing CustomBatchConfigurer with SQLite optimizations");
    }

    @Bean
    @Primary
    @Override
    public @NonNull PlatformTransactionManager getTransactionManager() {
        SQLiteTransactionManager transactionManager = new SQLiteTransactionManager(dataSource);
        log.info("Created custom SQLite transaction manager with optimized settings");
        return transactionManager;
    }
    
    @Override
    @Bean
    @Primary
    public @NonNull DataSource getDataSource() {
        return this.dataSource;
    }
    
    @Override
    public @NonNull Isolation getIsolationLevelForCreate() {
        return Isolation.READ_COMMITTED;
    }
    
    @Bean
    public ExecutionContextSerializer executionContextSerializer() {
        // Use Jackson serializer for better handling of large contexts
        Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
        log.info("Created custom execution context serializer for SQLite");
        return serializer;
    }
    
    @Override
    protected int getMaxVarCharLength() {
        // Increase the maximum varchar length for context serialization
        return 2500;
    }
}
