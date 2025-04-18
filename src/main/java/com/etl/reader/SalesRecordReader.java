package com.etl.reader;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.lang.NonNull;

import com.etl.model.SalesRecord;

public class SalesRecordReader implements ItemReader<SalesRecord>, ItemStream {
    private final FlatFileItemReader<SalesRecord> delegate;
    private String currentFilePath;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public SalesRecordReader(String initialFilePath) {
        this.currentFilePath = initialFilePath;
        this.delegate = createReader(initialFilePath);
    }

    private FlatFileItemReader<SalesRecord> createReader(String filePath) {
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(new Converter<String, LocalDateTime>() {
            @Override
            public LocalDateTime convert(String source) {
                return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
            }
        });

        BeanWrapperFieldSetMapper<SalesRecord> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesRecord.class);
        fieldSetMapper.setConversionService(conversionService);

        return new FlatFileItemReaderBuilder<SalesRecord>()
                .name("salesRecordReader")
                .resource(new FileSystemResource(new File(filePath)))
                .delimited()
                .names("productId", "productName", "price", "quantity", "saleDate", "customerId", "storeId")
                .fieldSetMapper(fieldSetMapper)
                .linesToSkip(1) // Skip header row
                .build();
    }

    public synchronized void setFilePath(String filePath) {
        this.currentFilePath = filePath;
        this.delegate.setResource(new FileSystemResource(new File(filePath)));
        this.initialized.set(false);
    }

    @Override
    public synchronized SalesRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!initialized.get()) {
            this.delegate.open(null);
            this.initialized.set(true);
        }
        return delegate.read();
    }

    @Override
    public synchronized void open(@NonNull org.springframework.batch.item.ExecutionContext executionContext) {
        delegate.open(executionContext);
        initialized.set(true);
    }

    @Override
    public synchronized void update(@NonNull org.springframework.batch.item.ExecutionContext executionContext) {
        delegate.update(executionContext);
    }

    @Override
    public synchronized void close() {
        delegate.close();
        initialized.set(false);
    }
} 