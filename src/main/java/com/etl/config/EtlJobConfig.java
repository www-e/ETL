package com.etl.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;

import com.etl.model.SalesRecord;
import com.etl.processor.SalesRecordProcessor;
import com.etl.reader.SalesRecordReader;
import com.etl.writer.SalesRecordWriter;

@Configuration
@EnableBatchProcessing
public class EtlJobConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("etl-thread-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Job etlJob(JobRepository jobRepository, Step etlStep) {
        return new JobBuilder("salesEtlJob", jobRepository)
                .start(etlStep)
                .build();
    }

    @Bean
    public Step etlStep(JobRepository jobRepository, 
                       PlatformTransactionManager transactionManager,
                       ItemReader<SalesRecord> reader,
                       ItemProcessor<SalesRecord, SalesRecord> processor,
                       ItemWriter<SalesRecord> writer,
                       TaskExecutor taskExecutor) {
        return new StepBuilder("salesEtlStep", jobRepository)
                .<SalesRecord, SalesRecord>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor)
                .throttleLimit(4) // Limit concurrent threads
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SalesRecord> reader() {
        return new SalesRecordReader("");
    }

    @Bean
    @StepScope
    public ItemProcessor<SalesRecord, SalesRecord> processor() {
        return new SalesRecordProcessor();
    }

    @Bean
    @StepScope
    public ItemWriter<SalesRecord> writer(JdbcTemplate jdbcTemplate) {
        return new SalesRecordWriter(jdbcTemplate);
    }

    @Bean
    public ResourceDatabasePopulator databasePopulator() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        return populator;
    }

    @Bean
    public boolean initializeDatabase(DataSource dataSource, ResourceDatabasePopulator populator) {
        populator.execute(dataSource);
        return true;
    }
} 