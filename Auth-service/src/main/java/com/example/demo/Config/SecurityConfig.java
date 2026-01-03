package com.example.demo.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


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
    // Add this inside your SecurityConfig in Auth Service
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
            if (realmAccess == null || realmAccess.isEmpty()) {
                return new ArrayList<>();
            }
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // "ADMIN" -> "ROLE_ADMIN"
                    .collect(Collectors.toList());
        });
        return converter;
    }
}