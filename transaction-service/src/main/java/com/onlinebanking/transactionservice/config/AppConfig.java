package com.onlinebanking.transactionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
