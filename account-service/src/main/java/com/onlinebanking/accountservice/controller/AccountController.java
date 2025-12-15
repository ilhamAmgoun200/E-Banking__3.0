package com.onlinebanking.accountservice.controller;

import com.onlinebanking.accountservice.model.Account;
import com.onlinebanking.accountservice.service.AccountService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/user/{username}")
    public List<Account> getAccountsByUsername(@PathVariable String username) {
        return accountService.getAccountsByUsername(username);
    }

    @GetMapping("/number/{accountNumber}")
    public Account getAccountByAccountNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByAccountNumber(accountNumber);
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<?> deposit(@PathVariable String accountNumber, @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }
        
        Account updatedAccount = accountService.deposit(accountNumber, amount);
        if (updatedAccount != null) {
            return ResponseEntity.ok(updatedAccount);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String accountNumber, @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }
        
        Account updatedAccount = accountService.withdraw(accountNumber, amount);
        if (updatedAccount != null) {
            return ResponseEntity.ok(updatedAccount);
        } else {
            return ResponseEntity.badRequest().body("Insufficient funds or account not found");
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> request) {
        String fromAccountNumber = (String) request.get("fromAccountNumber");
        String toAccountNumber = (String) request.get("toAccountNumber");
        Double amount = (Double) request.get("amount");
        
        Map<String, Object> response = new java.util.HashMap<>();
        
        if (fromAccountNumber == null || toAccountNumber == null || amount == null || amount <= 0) {
            response.put("error", "Invalid transfer parameters");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (fromAccountNumber.equals(toAccountNumber)) {
            response.put("error", "Cannot transfer to the same account");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = accountService.transfer(fromAccountNumber, toAccountNumber, amount);
        if (success) {
            response.put("success", true);
            response.put("message", "Transfer successful");
            response.put("fromAccount", fromAccountNumber);
            response.put("toAccount", toAccountNumber);
            response.put("amount", amount);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Transfer failed: Account not found or insufficient funds");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/exists/{accountNumber}")
    public ResponseEntity<Boolean> checkAccountExists(@PathVariable String accountNumber) {
        boolean exists = accountService.accountExists(accountNumber);
        return ResponseEntity.ok(exists);
    }
}
