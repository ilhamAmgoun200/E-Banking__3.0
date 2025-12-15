package com.onlinebanking.portal.controller;

import com.onlinebanking.portal.model.Account;
import com.onlinebanking.portal.service.AccountService;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
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

    @GetMapping("/ui")
    public String getAccountsUI(Model model) {
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "accounts"; 
    }

}