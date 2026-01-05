package com.onlinebanking.accountservice.service;

import com.onlinebanking.accountservice.model.Account;
import com.onlinebanking.accountservice.model.Role;
import com.onlinebanking.accountservice.model.AccountStatus;
import com.onlinebanking.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // Valeurs par défaut si non spécifiées
        if (account.getRole() == null) {
            account.setRole(Role.CLIENT);
        }
        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        if (account.getBalance() == null) {
            account.setBalance(0.0);
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

    // NOUVELLES MÉTHODES
    public List<Account> getAccountsByRole(Role role) {
        return accountRepository.findByRole(role);
    }

    public List<Account> getAccountsByStatus(AccountStatus status) {
        return accountRepository.findByStatus(status);
    }

    public List<Account> getAccountsByRoleAndStatus(Role role, AccountStatus status) {
        return accountRepository.findByRoleAndStatus(role, status);
    }

    public long countByRole(Role role) {
        return accountRepository.countByRole(role);
    }

    public long countByStatus(AccountStatus status) {
        return accountRepository.countByStatus(status);
    }

    public long countByRoleAndStatus(Role role, AccountStatus status) {
        return accountRepository.countByRoleAndStatus(role, status);
    }

    @Transactional
    public Account deposit(String accountNumber, Double amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account != null && account.getStatus() == AccountStatus.ACTIVE) {
            account.setBalance(account.getBalance() + amount);
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public Account withdraw(String accountNumber, Double amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account != null && account.getStatus() == AccountStatus.ACTIVE) {
            if (account.getBalance() >= amount) {
                account.setBalance(account.getBalance() - amount);
                return accountRepository.save(account);
            }
        }
        return null;
    }

    @Transactional
    public boolean transfer(String fromAccountNumber, String toAccountNumber, Double amount) {
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber);
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber);

        if (fromAccount != null && toAccount != null &&
                fromAccount.getStatus() == AccountStatus.ACTIVE &&
                toAccount.getStatus() == AccountStatus.ACTIVE) {
            if (fromAccount.getBalance() >= amount) {
                fromAccount.setBalance(fromAccount.getBalance() - amount);
                toAccount.setBalance(toAccount.getBalance() + amount);
                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);
                return true;
            }
        }
        return false;
    }

    public boolean accountExists(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber) != null;
    }

    @Transactional
    public Account updateAccount(Long id, Account accountDetails) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            account.setAccountNumber(accountDetails.getAccountNumber());
            account.setAccountHolderName(accountDetails.getAccountHolderName());
            account.setUsername(accountDetails.getUsername());
            account.setBalance(accountDetails.getBalance());

            // Mettre à jour le rôle et le statut
            if (accountDetails.getRole() != null) {
                account.setRole(accountDetails.getRole());
            }
            if (accountDetails.getStatus() != null) {
                account.setStatus(accountDetails.getStatus());
            }

            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public boolean deleteAccount(Long id) {
        if (accountRepository.existsById(id)) {
            accountRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public Account updateAccountStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            account.setStatus(status);
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public Account updateAccountRole(Long id, Role role) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            account.setRole(role);
            return accountRepository.save(account);
        }
        return null;
    }
}