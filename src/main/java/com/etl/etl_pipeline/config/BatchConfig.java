package com.etl.etl_pipeline.config;

import com.etl.etl_pipeline.model.InputData;
import com.etl.etl_pipeline.model.ProcessedData;
import com.etl.etl_pipeline.processor.DataProcessor;
import com.etl.etl_pipeline.reader.FileReaderFactory;
import com.etl.etl_pipeline.writer.DatabaseWriter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

@Configuration
public class BatchConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FileReaderFactory fileReaderFactory;

    @Autowired
    private DataProcessor dataProcessor;

    @Autowired
    private DatabaseWriter databaseWriter;

    @Value("${etl.chunk-size:10}")
    private int chunkSize;

    @Value("${etl.max-threads:4}")
    private int maxThreads;
    
    @Value("${etl.throttle-limit:4}")
    private int throttleLimit;
    
    @Value("${etl.queue-capacity:16}")
    private int queueCapacity;

    @Bean
    public TaskExecutor taskExecutor() {
        // Configure task executor with reduced concurrency to prevent database locking
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor = 
            new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        
        // Reduce thread count to minimize database contention
        taskExecutor.setCorePoolSize(maxThreads);
        taskExecutor.setMaxPoolSize(maxThreads);
        taskExecutor.setQueueCapacity(queueCapacity);
        taskExecutor.setThreadNamePrefix("etl-thread-");
        taskExecutor.setAllowCoreThreadTimeOut(true);
        taskExecutor.setKeepAliveSeconds(120); // Longer idle threads timeout
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(60);
        taskExecutor.initialize();
        
        return taskExecutor;
    }

    @Bean
    public Job etlJob() {
        return new JobBuilder("etlJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(etlStep())
                .build();
    }

    @Bean
    public Step etlStep() {
        return new StepBuilder("etlStep", jobRepository)
                .<InputData, ProcessedData>chunk(chunkSize, transactionManager)
                .reader(reader(null)) // This will be replaced at runtime with the actual reader
                .processor(processor())
                .writer(writer())
                .taskExecutor(taskExecutor())
                // Note: throttleLimit is deprecated in Spring Batch 5.0+
                // The ThreadPoolTaskExecutor configuration now handles this
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<InputData> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        if (filePath != null) {
            try {
                ItemReader<InputData> reader = fileReaderFactory.getReader(filePath);
                // Initialize the reader
                if (reader instanceof org.springframework.batch.item.ItemStream) {
                    ((org.springframework.batch.item.ItemStream) reader).open(new org.springframework.batch.item.ExecutionContext());
                }
                return reader;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create reader for file: " + filePath, e);
            }
        } else {
            return defaultReader();
        }
    }
    
    @Bean
    public ItemReader<InputData> defaultReader() {
        // This is a placeholder reader that returns an empty list
        return new ListItemReader<>(Collections.emptyList());
    }

    @Bean
    public ItemProcessor<InputData, ProcessedData> processor() {
        return dataProcessor;
    }

    @Bean
    public ItemWriter<ProcessedData> writer() {
        return databaseWriter;
    }
}
