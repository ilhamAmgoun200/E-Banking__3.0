package com.onlinebanking.uiservice.controller;

import com.onlinebanking.uiservice.service.AccountServiceClientWithCircuitBreaker;
import com.onlinebanking.uiservice.service.TransactionServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Controller
@SessionAttributes("username")
public class AccountController {
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private AccountServiceClientWithCircuitBreaker accountServiceClient;
    
    @Autowired
    private TransactionServiceClient transactionServiceClient;

    @GetMapping("/accounts")
    public String accounts(Model model, @SessionAttribute(value = "username", required = false) String username) {
        // Check if user is logged in
        if (username == null || username.isEmpty()) {
            return "redirect:/login";
        }
        
        try {
            // Use circuit breaker service to fetch accounts
            List<Map<String, Object>> accounts = accountServiceClient.getAccountsByUsername(username);
            
            // Handle null response
            if (accounts == null) {
                accounts = new ArrayList<>();
            }
            
            model.addAttribute("accounts", accounts);
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            // Circuit breaker is open
            System.err.println("Account service circuit breaker is open: " + e.getMessage());
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("error", "Account service is currently unavailable due to circuit breaker. Please try again later.");
        } catch (Exception e) {
            // Service unavailable or any other error
            System.err.println("Account service error: " + e.getMessage());
            model.addAttribute("accounts", new ArrayList<>());
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpError = (org.springframework.web.client.HttpClientErrorException) e;
                model.addAttribute("error", "Error fetching accounts: " + httpError.getStatusCode() + " - " + httpError.getStatusText());
            } else {
                model.addAttribute("error", "Account service is currently unavailable. Error: " + e.getMessage());
            }
        }
        
        return "accounts";
    }

    // Health check endpoint for testing services
    @GetMapping("/test-services")
    @ResponseBody
    public ResponseEntity<String> testServices() {
        StringBuilder result = new StringBuilder();
        
        try {
            // Test Account Service
            String accountUrl = "http://localhost:8081/accounts";
            Object accountResponse = restTemplate.getForObject(accountUrl, Object.class);
            result.append("✅ Account Service (8081): OK\n");
        } catch (Exception e) {
            result.append("❌ Account Service (8081): " + e.getMessage() + "\n");
        }
        
        try {
            // Test Transaction Service
            String transactionUrl = "http://localhost:8082/transactions";
            Object transactionResponse = restTemplate.getForObject(transactionUrl, Object.class);
            result.append("✅ Transaction Service (8082): OK\n");
        } catch (Exception e) {
            result.append("❌ Transaction Service (8082): " + e.getMessage() + "\n");
        }
        
        return ResponseEntity.ok(result.toString());
    }

    // Debug endpoint for testing transaction filtering
    @GetMapping("/debug-transactions")
    @ResponseBody
    public ResponseEntity<String> debugTransactions(@RequestParam(required = false) String username,
                                                   @RequestParam(required = false) String accountNumber) {
        StringBuilder result = new StringBuilder();
        result.append("=== TRANSACTION DEBUG ENDPOINT ===\n\n");
        
        if (username == null) username = "testuser"; // default for testing
        
        result.append("Testing with username: ").append(username).append("\n");
        result.append("Testing with accountNumber: ").append(accountNumber != null ? accountNumber : "ALL").append("\n\n");
        
        try {
            if (accountNumber != null && !accountNumber.isEmpty()) {
                String url = "http://localhost:8082/transactions/account/" + accountNumber;
                result.append("Testing URL: ").append(url).append("\n");
                List<Map<String, Object>> transactions = restTemplate.getForObject(url, List.class);
                result.append("Result: ").append(transactions != null ? transactions.size() + " transactions found" : "null response").append("\n");
                if (transactions != null && !transactions.isEmpty()) {
                    result.append("Sample transaction: ").append(transactions.get(0).toString()).append("\n");
                }
            } else {
                String url = "http://localhost:8082/transactions/user/" + username;
                result.append("Testing URL: ").append(url).append("\n");
                List<Map<String, Object>> transactions = restTemplate.getForObject(url, List.class);
                result.append("Result: ").append(transactions != null ? transactions.size() + " transactions found" : "null response").append("\n");
                if (transactions != null && !transactions.isEmpty()) {
                    result.append("Sample transaction: ").append(transactions.get(0).toString()).append("\n");
                }
            }
        } catch (Exception e) {
            result.append("ERROR: ").append(e.getMessage()).append("\n");
            result.append("Exception type: ").append(e.getClass().getSimpleName()).append("\n");
        }
        
        return ResponseEntity.ok(result.toString());
    }

    @GetMapping("/transactions")
    public String transactions(Model model, 
                             @SessionAttribute(value = "username", required = false) String username,
                             @RequestParam(required = false) String accountNumber,
                             HttpSession session) {
        
        // Debug logging
        System.out.println("=== TRANSACTIONS DEBUG ===");
        System.out.println("Username from session: " + username);
        System.out.println("Account number parameter: " + accountNumber);
        
        // Check if user is logged in
        if (username == null || username.isEmpty()) {
            System.out.println("No username in session, redirecting to login");
            return "redirect:/login";
        }
        
        List<Map<String, Object>> transactions = null;
        String serviceUrl = "";
        
        try {
            if (accountNumber != null && !accountNumber.isEmpty()) {
                // Fetch transactions for a specific account using circuit breaker
                System.out.println("Fetching transactions for account: " + accountNumber);
                transactions = transactionServiceClient.getTransactionsByAccountNumber(accountNumber);
            } else {
                // Fetch all transactions for the logged-in user using circuit breaker
                System.out.println("Fetching all transactions for user: " + username);
                transactions = transactionServiceClient.getTransactionsByUsername(username);
            }
            
            System.out.println("Transactions received: " + (transactions != null ? transactions.size() : "null"));
            
            // Handle null response
            if (transactions == null) {
                transactions = new ArrayList<>();
                System.out.println("Null response from service, using empty list");
            }
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Service unavailable
            System.err.println("Transaction service unavailable: " + e.getMessage());
            transactions = new ArrayList<>();
            model.addAttribute("error", "Transaction service is currently unavailable. Please ensure the transaction service is running on port 8082. Service URL: " + serviceUrl);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // HTTP error (4xx, 5xx)
            System.err.println("HTTP error fetching transactions: " + e.getMessage());
            transactions = new ArrayList<>();
            if (e.getStatusCode().value() == 404) {
                if (accountNumber != null) {
                    model.addAttribute("error", "No transactions found for account: " + accountNumber);
                } else {
                    model.addAttribute("error", "No transactions found for user: " + username);
                }
            } else {
                model.addAttribute("error", "Error fetching transactions: " + e.getStatusCode() + " - " + e.getStatusText() + ". Service URL: " + serviceUrl);
            }
        } catch (Exception e) {
            // Other errors
            System.err.println("Error fetching transactions: " + e.getMessage());
            e.printStackTrace();
            transactions = new ArrayList<>();
            model.addAttribute("error", "Unexpected error occurred: " + e.getMessage() + ". Service URL: " + serviceUrl);
        }
        
        System.out.println("Final transactions count: " + transactions.size());
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("selectedAccountNumber", accountNumber);
        return "transactions";
    }

    @PostMapping("/deposit")
    @ResponseBody
    public ResponseEntity<String> deposit(@RequestParam String accountNumber, 
                                        @RequestParam Double amount,
                                        @SessionAttribute(value = "username", required = false) String username) {
        // Check if user is logged in
        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please log in first");
        }
        
        try {
            // Call account service to deposit money using circuit breaker
            Map<String, Object> accountResponse = accountServiceClient.deposit(accountNumber, amount);
            
            if (accountResponse != null && !accountResponse.containsKey("error")) {
                // Create transaction record using circuit breaker
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("accountNumber", accountNumber);
                transaction.put("amount", amount);
                transaction.put("type", "DEPOSIT");
                transaction.put("timestamp", LocalDateTime.now());
                transaction.put("username", username);
                
                try {
                    Map<String, Object> transactionResponse = transactionServiceClient.createTransaction(transaction);
                    return ResponseEntity.ok("Deposit successful");
                } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException ex) {
                    return ResponseEntity.ok("Deposit successful, but transaction logging is temporarily unavailable");
                }
            } else {
                return ResponseEntity.badRequest().body("Deposit failed");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/withdraw")
    @ResponseBody
    public ResponseEntity<String> withdraw(@RequestParam String accountNumber, 
                                         @RequestParam Double amount,
                                         @SessionAttribute(value = "username", required = false) String username) {
        // Check if user is logged in
        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please log in first");
        }
        
        try {
            // Call account service to withdraw money using circuit breaker
            Map<String, Object> accountResponse = accountServiceClient.withdraw(accountNumber, amount);
            
            if (accountResponse != null && !accountResponse.containsKey("error")) {
                // Create transaction record using circuit breaker
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("accountNumber", accountNumber);
                transaction.put("amount", amount);
                transaction.put("type", "WITHDRAWAL");
                transaction.put("timestamp", LocalDateTime.now());
                transaction.put("username", username);
                
                try {
                    Map<String, Object> transactionResponse = transactionServiceClient.createTransaction(transaction);
                    return ResponseEntity.ok("Withdrawal successful");
                } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException ex) {
                    return ResponseEntity.ok("Withdrawal successful, but transaction logging is temporarily unavailable");
                }
            } else {
                return ResponseEntity.badRequest().body("Insufficient funds or invalid account");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/transfer")
    @ResponseBody
    public ResponseEntity<String> transfer(@RequestParam String accountNumber,
                          @RequestParam String toAccountNumber,
                          @RequestParam Double amount,
                          @RequestParam(required = false) String description,
                          HttpSession session) {
        try {
            // Get current user from session
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please log in first");
            }
            
            // Validate amount
            if (amount == null || amount <= 0) {
                return ResponseEntity.badRequest().body("Transfer amount must be greater than zero");
            }
            
            // Check if destination account exists
            Boolean toAccountExists = accountServiceClient.checkAccountExists(toAccountNumber);
            if (toAccountExists == null || !toAccountExists) {
                return ResponseEntity.badRequest().body("Destination account " + toAccountNumber + " not found in database");
            }
            
            // Get destination account details to get username
            List<Map<String, Object>> allAccounts = accountServiceClient.getAllAccounts();
            String toUsername = null;
            for (Map<String, Object> account : allAccounts) {
                if (toAccountNumber.equals(String.valueOf(account.get("accountNumber")))) {
                    toUsername = (String) account.get("username");
                    break;
                }
            }
            
            if (toUsername == null) {
                return ResponseEntity.badRequest().body("Unable to identify destination account owner");
            }
            
            // Perform the transfer
            Map<String, Object> transferResult = accountServiceClient.transfer(accountNumber, toAccountNumber, amount);
            
            if (transferResult != null && transferResult.containsKey("error")) {
                return ResponseEntity.badRequest().body(transferResult.get("error").toString());
            }
            
            // Create transfer transactions in transaction service
            try {
                List<Map<String, Object>> transactionResult = transactionServiceClient.createTransferTransactions(
                    accountNumber, toAccountNumber, amount, username, toUsername, description != null ? description : ""
                );
                
                if (transactionResult != null && !transactionResult.isEmpty() && 
                    transactionResult.get(0).containsKey("error")) {
                    System.err.println("Failed to log transfer transactions: " + transactionResult.get(0).get("error"));
                }
            } catch (Exception e) {
                System.err.println("Failed to log transfer transactions: " + e.getMessage());
                // Don't fail the transfer if logging fails
            }
            
            return ResponseEntity.ok("Transfer of $" + amount + " to account " + toAccountNumber + " successful!");
            
        } catch (Exception e) {
            System.err.println("Transfer error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Transfer failed: " + e.getMessage());
        }
    }
}
