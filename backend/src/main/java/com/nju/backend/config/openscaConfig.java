package com.nju.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class openscaConfig {
    @Value("${opensca.tool-path}")
    private String openscaToolPath;

    @Bean
    public String getOpenscaToolPath() {
        return openscaToolPath;
    }
}
