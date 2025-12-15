package com.onlinebanking.transactionservice.controller;

import com.onlinebanking.transactionservice.model.Transaction;
import com.onlinebanking.transactionservice.service.TransactionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction);
    }

    @GetMapping("/user/{username}")
    public List<Transaction> getTransactionsByUsername(@PathVariable String username) {
        return transactionService.getTransactionsByUsername(username);
    }

    @GetMapping("/account/{accountNumber}")
    public List<Transaction> getTransactionsByAccountNumber(@PathVariable String accountNumber) {
        return transactionService.getTransactionsByAccountNumber(accountNumber);
    }
    
    @GetMapping("/account/{accountNumber}/all")
    public List<Transaction> getAllTransactionsForAccount(@PathVariable String accountNumber) {
        return transactionService.getAllTransactionsForAccount(accountNumber);
    }
    
    @PostMapping("/transfer")
    public List<Transaction> createTransferTransactions(@RequestBody java.util.Map<String, Object> request) {
        String fromAccountNumber = (String) request.get("fromAccountNumber");
        String toAccountNumber = (String) request.get("toAccountNumber");
        Double amount = (Double) request.get("amount");
        String fromUsername = (String) request.get("fromUsername");
        String toUsername = (String) request.get("toUsername");
        String description = (String) request.get("description");
        
        return transactionService.createTransferTransactions(fromAccountNumber, toAccountNumber, 
                                                           amount, fromUsername, toUsername, description);
    }
}
