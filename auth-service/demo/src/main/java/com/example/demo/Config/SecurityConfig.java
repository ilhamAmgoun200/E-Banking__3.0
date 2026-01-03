package com.example.demo.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Syntaxe Spring Boot 3
                .authorizeHttpRequests(auth -> auth
                        // 🔓 On laisse passer l'inscription et le login
                        .requestMatchers("/api/auth/**").permitAll()
                        // 🔒 Le reste est fermé
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}