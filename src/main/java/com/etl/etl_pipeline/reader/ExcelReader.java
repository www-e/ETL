package com.etl.etl_pipeline.reader;

import com.etl.etl_pipeline.model.InputData;
import com.etl.etl_pipeline.util.DateUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Reader for Excel files (both .xls and .xlsx)
 */
@Component
public class ExcelReader {

    /**
     * Creates a reader for Excel files
     * @param filePath Path to the Excel file
     * @return ItemReader for Excel files
     */
    public ItemReader<InputData> createReader(String filePath) {
        return new ExcelItemReader(filePath);
    }

    /**
     * Custom Excel item reader implementation
     */
    private static class ExcelItemReader extends AbstractItemCountingItemStreamItemReader<InputData> {
        private final String filePath;
        private Workbook workbook;
        private Sheet sheet;
        private Iterator<Row> rowIterator;
        private boolean initialized = false;

        public ExcelItemReader(String filePath) {
            this.filePath = filePath;
            setName("excelItemReader");
        }

        @Override
        protected void doOpen() throws Exception {
            if (!initialized) {
                workbook = WorkbookFactory.create(new java.io.File(filePath));
                sheet = workbook.getSheetAt(0); // Use first sheet
                rowIterator = sheet.rowIterator();
                
                // Skip header row
                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }
                
                initialized = true;
            }
        }

        @Override
        protected InputData doRead() throws Exception {
            if (rowIterator == null || !rowIterator.hasNext()) {
                return null;
            }

            Row row = rowIterator.next();
            return mapRowToInputData(row);
        }

        private InputData mapRowToInputData(Row row) {
            InputData data = new InputData();
            
            // Map Excel cells to InputData fields
            data.setId(getCellValueAsString(row.getCell(0)));
            data.setFirstName(getCellValueAsString(row.getCell(1)));
            data.setLastName(getCellValueAsString(row.getCell(2)));
            data.setEmail(getCellValueAsString(row.getCell(3)));
            
            // Parse date
            String birthDateStr = getCellValueAsString(row.getCell(4));
            data.setBirthDate(DateUtils.parseDate(birthDateStr));
            
            data.setAddress(getCellValueAsString(row.getCell(5)));
            data.setCity(getCellValueAsString(row.getCell(6)));
            data.setCountry(getCellValueAsString(row.getCell(7)));
            data.setPhoneNumber(getCellValueAsString(row.getCell(8)));
            
            // Parse numeric values
            data.setSalary(getCellValueAsDouble(row.getCell(9)));
            data.setDependents(getCellValueAsInteger(row.getCell(10)));
            
            return data;
        }

        private String getCellValueAsString(Cell cell) {
            if (cell == null) {
                return "";
            }
            
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    return String.valueOf(cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return "";
            }
        }

        private Double getCellValueAsDouble(Cell cell) {
            if (cell == null) {
                return 0.0;
            }
            
            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return cell.getNumericCellValue();
                } else if (cell.getCellType() == CellType.STRING) {
                    return Double.parseDouble(cell.getStringCellValue());
                }
            } catch (Exception e) {
                // Return default value if parsing fails
            }
            
            return 0.0;
        }

        private Integer getCellValueAsInteger(Cell cell) {
            if (cell == null) {
                return 0;
            }
            
            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return (int) cell.getNumericCellValue();
                } else if (cell.getCellType() == CellType.STRING) {
                    return Integer.parseInt(cell.getStringCellValue());
                }
            } catch (Exception e) {
                // Return default value if parsing fails
            }
            
            return 0;
        }

        @Override
        protected void doClose() throws Exception {
            if (workbook != null) {
                workbook.close();
                workbook = null;
            }
            rowIterator = null;
            sheet = null;
            initialized = false;
        }
    }
}
