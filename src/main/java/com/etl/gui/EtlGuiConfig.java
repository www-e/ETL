package com.etl.gui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EtlGuiConfig {
    @Bean
    public EtlGuiService etlGuiService(JdbcTemplate jdbcTemplate) {
        return new EtlGuiService(jdbcTemplate);
    }
} 