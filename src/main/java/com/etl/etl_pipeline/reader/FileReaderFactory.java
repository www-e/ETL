package com.etl.etl_pipeline.reader;

import com.etl.etl_pipeline.model.InputData;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for creating appropriate readers based on file type
 */
@Component
public class FileReaderFactory {

    @Autowired
    private CsvReader csvReader;

    @Autowired
    private ExcelReader excelReader;

    @Autowired
    private JsonReader jsonReader;

    /**
     * Returns the appropriate reader based on file extension
     * @param filePath Path to the input file
     * @return ItemReader for the specified file type
     */
    public ItemReader<InputData> getReader(String filePath) {
        if (filePath == null) {
            return null;
        }

        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".csv")) {
            return csvReader.createReader(filePath);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return excelReader.createReader(filePath);
        } else if (fileName.endsWith(".json")) {
            return jsonReader.createReader(filePath);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }
}
