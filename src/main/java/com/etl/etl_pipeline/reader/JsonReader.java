package com.etl.etl_pipeline.reader;

import com.etl.etl_pipeline.model.InputData;
import com.etl.etl_pipeline.util.DateUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Reader for JSON files
 */
@Component
public class JsonReader {

    /**
     * Creates a reader for JSON files
     * @param filePath Path to the JSON file
     * @return ItemReader for JSON files
     */
    public ItemReader<InputData> createReader(String filePath) {
        return new JsonItemReader(filePath);
    }

    /**
     * Custom JSON item reader implementation
     */
    private static class JsonItemReader extends AbstractItemCountingItemStreamItemReader<InputData> {
        private final String filePath;
        private Iterator<ObjectNode> jsonIterator;
        private boolean initialized = false;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public JsonItemReader(String filePath) {
            this.filePath = filePath;
            setName("jsonItemReader");
        }

        @Override
        protected void doOpen() throws Exception {
            if (!initialized) {
                try {
                    // Read JSON file as a list of objects
                    List<ObjectNode> jsonObjects = objectMapper.readValue(
                        new File(filePath), 
                        new TypeReference<List<ObjectNode>>() {}
                    );
                    
                    jsonIterator = jsonObjects.iterator();
                    initialized = true;
                    
                    // Log successful initialization
                    System.out.println("Successfully initialized JSON reader with " + 
                                     (jsonObjects != null ? jsonObjects.size() : 0) + " records");
                } catch (Exception e) {
                    System.err.println("Error initializing JSON reader: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
        }

        @Override
        protected InputData doRead() throws Exception {
            if (jsonIterator == null || !jsonIterator.hasNext()) {
                return null;
            }

            ObjectNode jsonNode = jsonIterator.next();
            return mapJsonToInputData(jsonNode);
        }

        private InputData mapJsonToInputData(ObjectNode jsonNode) {
            InputData data = new InputData();
            
            // Map JSON fields to InputData fields
            data.setId(getStringValue(jsonNode, "id"));
            data.setFirstName(getStringValue(jsonNode, "firstName"));
            data.setLastName(getStringValue(jsonNode, "lastName"));
            data.setEmail(getStringValue(jsonNode, "email"));
            
            // Parse date
            String birthDateStr = getStringValue(jsonNode, "birthDate");
            data.setBirthDate(DateUtils.parseDate(birthDateStr));
            
            data.setAddress(getStringValue(jsonNode, "address"));
            data.setCity(getStringValue(jsonNode, "city"));
            data.setCountry(getStringValue(jsonNode, "country"));
            data.setPhoneNumber(getStringValue(jsonNode, "phoneNumber"));
            
            // Parse numeric values
            data.setSalary(getDoubleValue(jsonNode, "salary"));
            data.setDependents(getIntegerValue(jsonNode, "dependents"));
            
            return data;
        }

        private String getStringValue(ObjectNode node, String fieldName) {
            return node.has(fieldName) ? node.get(fieldName).asText() : "";
        }

        private Double getDoubleValue(ObjectNode node, String fieldName) {
            if (!node.has(fieldName)) {
                return 0.0;
            }
            
            try {
                return node.get(fieldName).asDouble();
            } catch (Exception e) {
                return 0.0;
            }
        }

        private Integer getIntegerValue(ObjectNode node, String fieldName) {
            if (!node.has(fieldName)) {
                return 0;
            }
            
            try {
                return node.get(fieldName).asInt();
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        protected void doClose() throws Exception {
            jsonIterator = null;
            initialized = false;
        }
    }
}
