package com.onlinebanking.portal.controller;

import com.onlinebanking.portal.model.Transaction;
import com.onlinebanking.portal.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller  // Use @Controller instead of @RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // -------------------- UI Endpoint --------------------
    @GetMapping("/ui")
    public String getTransactionsUI(Model model) {
        model.addAttribute("transactions", transactionService.getAllTransactions());
        return "transactions"; // Thymeleaf template
    }

    @PostMapping("/ui")
    public String createTransactionUI(
            @RequestParam Long accountId,
            @RequestParam double amount,
            @RequestParam String type,
            Model model
    ) {
        try {
            transactionService.createTransaction(accountId, amount, type);
            model.addAttribute("success", "Transaction completed successfully!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("transactions", transactionService.getAllTransactions());
        return "transactions"; // return same template
    }

    // -------------------- API Endpoint --------------------
    @GetMapping("/api")
    @ResponseBody // Only this method returns JSON for Postman
    public List<Transaction> getAllTransactionsAPI() {
        return transactionService.getAllTransactions();
    }

    @PostMapping("/api")
    @ResponseBody
    public Transaction createTransactionAPI(
            @RequestParam Long accountId,
            @RequestParam double amount,
            @RequestParam String type) {
        return transactionService.createTransaction(accountId, amount, type);
    }
}