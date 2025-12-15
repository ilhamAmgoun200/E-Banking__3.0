package com.onlinebanking.uiservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

@Service
public class UserServiceClientWithCircuitBreaker {
    
    private static final String USER_SERVICE_BASE_URL = "http://localhost:8084";
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    @Qualifier("userServiceCircuitBreaker")
    private CircuitBreaker userServiceCircuitBreaker;
    
    public Map<String, Object> login(Map<String, String> loginRequest) {
        Supplier<Map<String, Object>> loginCall = () -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);
                
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    USER_SERVICE_BASE_URL + "/api/login", 
                    entity, 
                    Map.class
                );
                
                return response.getBody();
            } catch (HttpClientErrorException e) {
                // Handle 4xx errors (like invalid credentials)
                Map<String, Object> errorResponse = new HashMap<>();
                if (e.getStatusCode().value() == 401) {
                    errorResponse.put("error", "Invalid username or password");
                } else {
                    errorResponse.put("error", "Authentication failed: " + e.getStatusText());
                }
                return errorResponse;
            } catch (ResourceAccessException e) {
                // Service is down - this will trigger circuit breaker
                throw new RuntimeException("User service is unavailable", e);
            }
        };
        
        return userServiceCircuitBreaker.executeSupplier(loginCall);
    }
    
    public Map<String, Object> loginWithFallback(Map<String, String> loginRequest) {
        try {
            return login(loginRequest);
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            // Circuit breaker is open
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("error", "Authentication service is temporarily unavailable. Please try again in a few moments.");
            fallbackResponse.put("circuitBreakerOpen", true);
            return fallbackResponse;
        } catch (Exception e) {
            // Other errors
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("error", "Authentication service error: " + e.getMessage());
            return fallbackResponse;
        }
    }
    
    public Map<String, Object> register(Map<String, String> registerRequest) {
        Supplier<Map<String, Object>> registerCall = () -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(registerRequest, headers);
                
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    USER_SERVICE_BASE_URL + "/api/register", 
                    entity, 
                    Map.class
                );
                
                return response.getBody();
            } catch (HttpClientErrorException e) {
                // Handle 4xx errors (like user already exists)
                Map<String, Object> errorResponse = new HashMap<>();
                if (e.getStatusCode().value() == 409) {
                    errorResponse.put("error", "Username already exists");
                } else {
                    errorResponse.put("error", "Registration failed: " + e.getStatusText());
                }
                return errorResponse;
            } catch (ResourceAccessException e) {
                // Service is down - this will trigger circuit breaker
                throw new RuntimeException("User service is unavailable", e);
            }
        };
        
        return userServiceCircuitBreaker.executeSupplier(registerCall);
    }
    
    public Map<String, Object> registerWithFallback(Map<String, String> registerRequest) {
        try {
            return register(registerRequest);
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            // Circuit breaker is open
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("error", "Registration service is temporarily unavailable. Please try again in a few moments.");
            fallbackResponse.put("circuitBreakerOpen", true);
            return fallbackResponse;
        } catch (Exception e) {
            // Other errors
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("error", "Registration service error: " + e.getMessage());
            return fallbackResponse;
        }
    }
    
    // Circuit breaker status methods
    public String getCircuitBreakerState() {
        return userServiceCircuitBreaker.getState().toString();
    }
    
    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return userServiceCircuitBreaker.getMetrics();
    }
}