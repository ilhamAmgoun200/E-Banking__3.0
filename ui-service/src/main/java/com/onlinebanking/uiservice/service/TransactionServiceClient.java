package com.onlinebanking.uiservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class TransactionServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private CircuitBreaker transactionServiceCircuitBreaker;
    
    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8082";
    
    public List<Map<String, Object>> getTransactionsByUsername(String username) {
        Supplier<List<Map<String, Object>>> supplier = () -> {
            String url = TRANSACTION_SERVICE_URL + "/transactions/user/" + username;
            return restTemplate.getForObject(url, List.class);
        };
        
        return transactionServiceCircuitBreaker.executeSupplier(supplier);
    }
    
    public List<Map<String, Object>> getTransactionsByAccountNumber(String accountNumber) {
        Supplier<List<Map<String, Object>>> supplier = () -> {
            String url = TRANSACTION_SERVICE_URL + "/transactions/account/" + accountNumber;
            return restTemplate.getForObject(url, List.class);
        };
        
        return transactionServiceCircuitBreaker.executeSupplier(supplier);
    }
    
    public Map<String, Object> createTransaction(Map<String, Object> transaction) {
        Supplier<Map<String, Object>> supplier = () -> {
            String url = TRANSACTION_SERVICE_URL + "/transactions";
            return restTemplate.postForObject(url, transaction, Map.class);
        };
        
        return transactionServiceCircuitBreaker.executeSupplier(supplier);
    }
    
    public List<Map<String, Object>> getAllTransactions() {
        Supplier<List<Map<String, Object>>> supplier = () -> {
            String url = TRANSACTION_SERVICE_URL + "/transactions";
            return restTemplate.getForObject(url, List.class);
        };
        
        return transactionServiceCircuitBreaker.executeSupplier(supplier);
    }
    
    public List<Map<String, Object>> createTransferTransactions(String fromAccountNumber, String toAccountNumber, 
                                                               Double amount, String fromUsername, String toUsername, String description) {
        Supplier<List<Map<String, Object>>> supplier = () -> {
            String url = TRANSACTION_SERVICE_URL + "/transactions/transfer";
            Map<String, Object> request = new HashMap<>();
            request.put("fromAccountNumber", fromAccountNumber);
            request.put("toAccountNumber", toAccountNumber);
            request.put("amount", amount);
            request.put("fromUsername", fromUsername);
            request.put("toUsername", toUsername);
            request.put("description", description);
            return restTemplate.postForObject(url, request, List.class);
        };
        
        return transactionServiceCircuitBreaker.executeSupplier(supplier);
    }
    
    public List<Map<String, Object>> getAllTransactionsForAccount(String accountNumber) {
        Supplier<List<Map<String, Object>>> supplier = () -> {
            String url = TRANSACTION_SERVICE_URL + "/transactions/account/" + accountNumber + "/all";
            return restTemplate.getForObject(url, List.class);
        };
        
        return transactionServiceCircuitBreaker.executeSupplier(supplier);
    }
    
    // Fallback methods
    public List<Map<String, Object>> getTransactionsByUsernameFallback(String username, Exception ex) {
        System.err.println("Transaction service circuit breaker activated for user: " + username + ". Error: " + ex.getMessage());
        List<Map<String, Object>> fallbackTransactions = new ArrayList<>();
        Map<String, Object> fallbackTransaction = new HashMap<>();
        fallbackTransaction.put("id", -1L);
        fallbackTransaction.put("accountNumber", "SERVICE_UNAVAILABLE");
        fallbackTransaction.put("amount", 0.0);
        fallbackTransaction.put("type", "SERVICE_ERROR");
        fallbackTransaction.put("timestamp", new java.util.Date());
        fallbackTransaction.put("username", username);
        fallbackTransaction.put("description", "Transaction service is currently unavailable");
        fallbackTransactions.add(fallbackTransaction);
        return fallbackTransactions;
    }
    
    public List<Map<String, Object>> getTransactionsByAccountNumberFallback(String accountNumber, Exception ex) {
        System.err.println("Transaction service circuit breaker activated for account: " + accountNumber + ". Error: " + ex.getMessage());
        List<Map<String, Object>> fallbackTransactions = new ArrayList<>();
        Map<String, Object> fallbackTransaction = new HashMap<>();
        fallbackTransaction.put("id", -1L);
        fallbackTransaction.put("accountNumber", accountNumber);
        fallbackTransaction.put("amount", 0.0);
        fallbackTransaction.put("type", "SERVICE_ERROR");
        fallbackTransaction.put("timestamp", new java.util.Date());
        fallbackTransaction.put("description", "Transaction service is currently unavailable");
        fallbackTransactions.add(fallbackTransaction);
        return fallbackTransactions;
    }
    
    public Map<String, Object> createTransactionFallback(Map<String, Object> transaction, Exception ex) {
        System.err.println("Transaction service circuit breaker activated for transaction creation. Error: " + ex.getMessage());
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("error", "Transaction service is currently unavailable. Transaction may not be recorded.");
        fallbackResponse.put("originalTransaction", transaction);
        return fallbackResponse;
    }
    
    public List<Map<String, Object>> getAllTransactionsFallback(Exception ex) {
        System.err.println("Transaction service circuit breaker activated for getAllTransactions. Error: " + ex.getMessage());
        List<Map<String, Object>> fallbackTransactions = new ArrayList<>();
        Map<String, Object> fallbackTransaction = new HashMap<>();
        fallbackTransaction.put("id", -1L);
        fallbackTransaction.put("accountNumber", "SERVICE_UNAVAILABLE");
        fallbackTransaction.put("amount", 0.0);
        fallbackTransaction.put("type", "SERVICE_ERROR");
        fallbackTransaction.put("timestamp", new java.util.Date());
        fallbackTransaction.put("description", "Transaction service is currently unavailable");
        fallbackTransactions.add(fallbackTransaction);
        return fallbackTransactions;
    }
    
    public List<Map<String, Object>> createTransferTransactionsFallback(String fromAccountNumber, String toAccountNumber, 
                                                                       Double amount, String fromUsername, String toUsername, String description, Exception ex) {
        System.err.println("Transaction service circuit breaker activated for transfer. Error: " + ex.getMessage());
        List<Map<String, Object>> fallbackResponse = new ArrayList<>();
        Map<String, Object> fallbackTransaction = new HashMap<>();
        fallbackTransaction.put("error", "Transaction logging service is currently unavailable.");
        fallbackTransaction.put("fromAccountNumber", fromAccountNumber);
        fallbackTransaction.put("toAccountNumber", toAccountNumber);
        fallbackTransaction.put("amount", amount);
        fallbackResponse.add(fallbackTransaction);
        return fallbackResponse;
    }
    
    public List<Map<String, Object>> getAllTransactionsForAccountFallback(String accountNumber, Exception ex) {
        System.err.println("Transaction service circuit breaker activated for getAllTransactionsForAccount. Account: " + accountNumber + ". Error: " + ex.getMessage());
        List<Map<String, Object>> fallbackTransactions = new ArrayList<>();
        Map<String, Object> fallbackTransaction = new HashMap<>();
        fallbackTransaction.put("id", -1L);
        fallbackTransaction.put("accountNumber", accountNumber);
        fallbackTransaction.put("amount", 0.0);
        fallbackTransaction.put("type", "SERVICE_ERROR");
        fallbackTransaction.put("timestamp", new java.util.Date());
        fallbackTransaction.put("description", "Transaction service is currently unavailable");
        fallbackTransactions.add(fallbackTransaction);
        return fallbackTransactions;
    }
}