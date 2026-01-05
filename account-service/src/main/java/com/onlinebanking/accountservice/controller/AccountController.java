package com.onlinebanking.accountservice.controller;

import com.onlinebanking.accountservice.model.Account;
import com.onlinebanking.accountservice.model.Role;
import com.onlinebanking.accountservice.model.AccountStatus;
import com.onlinebanking.accountservice.service.AccountService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    // NOUVELLES MÉTHODES POUR FILTRAGE PAR RÔLE ET STATUT
    @GetMapping("/role/{role}")
    public List<Account> getAccountsByRole(@PathVariable String role) {
        return accountService.getAccountsByRole(Role.valueOf(role.toUpperCase()));
    }

    @GetMapping("/status/{status}")
    public List<Account> getAccountsByStatus(@PathVariable String status) {
        return accountService.getAccountsByStatus(AccountStatus.valueOf(status.toUpperCase()));
    }

    @GetMapping("/filter")
    public List<Account> getAccountsByRoleAndStatus(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        if (role != null && status != null) {
            return accountService.getAccountsByRoleAndStatus(
                    Role.valueOf(role.toUpperCase()),
                    AccountStatus.valueOf(status.toUpperCase())
            );
        } else if (role != null) {
            return accountService.getAccountsByRole(Role.valueOf(role.toUpperCase()));
        } else if (status != null) {
            return accountService.getAccountsByStatus(AccountStatus.valueOf(status.toUpperCase()));
        }
        return accountService.getAllAccounts();
    }

    // STATISTIQUES
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total par rôle
        stats.put("totalClients", accountService.countByRole(Role.CLIENT));
        stats.put("totalAgents", accountService.countByRole(Role.AGENT));
        stats.put("totalAdmins", accountService.countByRole(Role.ADMIN));

        // Total par statut
        stats.put("totalActive", accountService.countByStatus(AccountStatus.ACTIVE));
        stats.put("totalInactive", accountService.countByStatus(AccountStatus.INACTIVE));

        // Détails par rôle et statut
        stats.put("activeClients", accountService.countByRoleAndStatus(Role.CLIENT, AccountStatus.ACTIVE));
        stats.put("activeAgents", accountService.countByRoleAndStatus(Role.AGENT, AccountStatus.ACTIVE));
        stats.put("activeAdmins", accountService.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE));

        stats.put("inactiveClients", accountService.countByRoleAndStatus(Role.CLIENT, AccountStatus.INACTIVE));
        stats.put("inactiveAgents", accountService.countByRoleAndStatus(Role.AGENT, AccountStatus.INACTIVE));
        stats.put("inactiveAdmins", accountService.countByRoleAndStatus(Role.ADMIN, AccountStatus.INACTIVE));

        stats.put("totalAccounts", accountService.getAllAccounts().size());

        return ResponseEntity.ok(stats);
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

        Map<String, Object> response = new HashMap<>();

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

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        Account updated = accountService.updateAccount(id, account);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        boolean deleted = accountService.deleteAccount(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // CHANGER LE STATUT D'UN COMPTE
    @PutMapping("/{id}/status")
    public ResponseEntity<Account> updateAccountStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }

        Account updated = accountService.updateAccountStatus(id, AccountStatus.valueOf(status.toUpperCase()));
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    // CHANGER LE RÔLE D'UN COMPTE
    @PutMapping("/{id}/role")
    public ResponseEntity<Account> updateAccountRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String role = request.get("role");
        if (role == null) {
            return ResponseEntity.badRequest().build();
        }

        Account updated = accountService.updateAccountRole(id, Role.valueOf(role.toUpperCase()));
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }
}