package com.onlinebanking.accountservice.service;

import com.onlinebanking.accountservice.model.Account;
import com.onlinebanking.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account createAccount(Account account) {
        // Gestion du rôle
        if (account.getRole() == null || account.getRole().isEmpty()) {
            account.setRole("CLIENT");
        } else {
            String r = account.getRole().toUpperCase();
            if (!r.equals("ADMIN") && !r.equals("AGENT") && !r.equals("CLIENT")) {
                account.setRole("CLIENT");
            } else {
                account.setRole(r);
            }
        }

        // Gestion du statut
        if (account.getStatus() == null || account.getStatus().isEmpty()) {
            account.setStatus("ACTIVE");
        }

        return accountRepository.save(account);
    }


    public Account getAccountById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    public List<Account> getAccountsByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    public Account getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Account deposit(String accountNumber, Double amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account != null) {
            account.setBalance(account.getBalance() + amount);
            return accountRepository.save(account);
        }
        return null;
    }

    public Account withdraw(String accountNumber, Double amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account != null && account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            return accountRepository.save(account);
        }
        return null;
    }
    
    public boolean transfer(String fromAccountNumber, String toAccountNumber, Double amount) {
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber);
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber);
        
        if (fromAccount != null && toAccount != null && fromAccount.getBalance() >= amount) {
            fromAccount.setBalance(fromAccount.getBalance() - amount);
            toAccount.setBalance(toAccount.getBalance() + amount);
            
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            return true;
        }
        return false;
    }
    
    public boolean accountExists(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        return account != null;
    }
}
