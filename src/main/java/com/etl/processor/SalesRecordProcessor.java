package com.etl.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.etl.model.SalesRecord;

@Component
public class SalesRecordProcessor implements ItemProcessor<SalesRecord, SalesRecord> {

    @Override
    public SalesRecord process(@NonNull SalesRecord item) throws Exception {
        // Calculate total amount
        double totalAmount = item.getPrice() * item.getQuantity();
        item.setTotalAmount(totalAmount);
        
        // Add any additional transformations here
        // For example: data validation, data enrichment, etc.
        
        return item;
    }
} 