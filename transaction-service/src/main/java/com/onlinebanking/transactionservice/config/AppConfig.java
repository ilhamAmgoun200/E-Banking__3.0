package com.onlinebanking.transactionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // Explicit no-args constructor
    public AppConfig() {
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
