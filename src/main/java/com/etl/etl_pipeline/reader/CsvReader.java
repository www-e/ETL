package com.etl.etl_pipeline.reader;

import com.etl.etl_pipeline.model.InputData;
import com.etl.etl_pipeline.util.DateUtils;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * Reader for CSV files
 */
@Component
public class CsvReader {

    /**
     * Creates a reader for CSV files
     * @param filePath Path to the CSV file
     * @return ItemReader for CSV files
     */
    public ItemReader<InputData> createReader(String filePath) {
        FlatFileItemReader<InputData> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1); // Skip header line
        reader.setLineMapper(createLineMapper());
        reader.setName("csvItemReader");
        
        try {
            // Initialize the reader
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize CSV reader", e);
        }
        
        return reader;
    }

    private LineMapper<InputData> createLineMapper() {
        DefaultLineMapper<InputData> lineMapper = new DefaultLineMapper<>();
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "firstName", "lastName", "email", "birthDate", 
                          "address", "city", "country", "phoneNumber", "salary", "dependents");
        
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> {
            InputData data = new InputData();
            
            // Set fields with appropriate type conversion
            data.setId(fieldSet.readString("id"));
            data.setFirstName(fieldSet.readString("firstName"));
            data.setLastName(fieldSet.readString("lastName"));
            data.setEmail(fieldSet.readString("email"));
            
            // Parse date using utility
            String birthDateStr = fieldSet.readString("birthDate");
            data.setBirthDate(DateUtils.parseDate(birthDateStr));
            
            data.setAddress(fieldSet.readString("address"));
            data.setCity(fieldSet.readString("city"));
            data.setCountry(fieldSet.readString("country"));
            data.setPhoneNumber(fieldSet.readString("phoneNumber"));
            
            try {
                data.setSalary(fieldSet.readDouble("salary"));
            } catch (Exception e) {
                data.setSalary(0.0);
            }
            
            try {
                data.setDependents(fieldSet.readInt("dependents"));
            } catch (Exception e) {
                data.setDependents(0);
            }
            
            return data;
        });
        
        return lineMapper;
    }
}
