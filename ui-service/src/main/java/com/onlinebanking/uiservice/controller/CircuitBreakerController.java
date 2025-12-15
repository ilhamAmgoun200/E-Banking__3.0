package com.onlinebanking.uiservice.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/circuit-breaker")
public class CircuitBreakerController {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Get status of all circuit breakers
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", circuitBreaker.getState().toString());
            cbStatus.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
            cbStatus.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
            cbStatus.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfNotPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
            
            status.put(circuitBreaker.getName(), cbStatus);
        });
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerInfo() {
        Map<String, Object> info = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            Map<String, Object> cbInfo = new HashMap<>();
            cbInfo.put("name", circuitBreaker.getName());
            cbInfo.put("state", circuitBreaker.getState().toString());
            cbInfo.put("config", getCircuitBreakerConfig(circuitBreaker));
            cbInfo.put("metrics", getCircuitBreakerMetrics(circuitBreaker));
            
            info.put(circuitBreaker.getName(), cbInfo);
        });
        
        return ResponseEntity.ok(info);
    }
    
    private Map<String, Object> getCircuitBreakerConfig(CircuitBreaker circuitBreaker) {
        Map<String, Object> config = new HashMap<>();
        config.put("slidingWindowSize", circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize());
        config.put("minimumNumberOfCalls", circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        config.put("failureRateThreshold", circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold());
        config.put("waitDurationInOpenState", circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState().toString());
        return config;
    }
    
    private Map<String, Object> getCircuitBreakerMetrics(CircuitBreaker circuitBreaker) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        metrics.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        metrics.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        metrics.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        metrics.put("numberOfNotPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
        return metrics;
    }
}