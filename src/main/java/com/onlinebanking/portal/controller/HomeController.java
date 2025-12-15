package com.onlinebanking.portal.controller;

import com.onlinebanking.portal.model.Account;
import com.onlinebanking.portal.model.Transaction;
import com.onlinebanking.portal.service.AccountService;
import com.onlinebanking.portal.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public HomeController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/accounts")
    public String accounts(Model model) {
        List<Account> accounts = accountService.getAllAccounts();
        model.addAttribute("accounts", accounts);
        return "accounts";
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        List<Transaction> transactions = transactionService.getAllTransactions();
        model.addAttribute("transactions", transactions);
        return "transactions";
    }
}