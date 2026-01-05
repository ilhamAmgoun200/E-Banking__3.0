package com.onlinebanking.accountservice.repository;

import com.onlinebanking.accountservice.model.Account;
import com.onlinebanking.accountservice.model.Role;
import com.onlinebanking.accountservice.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUsername(String username);
    Account findByAccountNumber(String accountNumber);

    // NOUVELLES MÉTHODES
    List<Account> findByRole(Role role);
    List<Account> findByStatus(AccountStatus status);
    List<Account> findByRoleAndStatus(Role role, AccountStatus status);

    // Compter par rôle
    long countByRole(Role role);

    // Compter par statut
    long countByStatus(AccountStatus status);

    // Compter par rôle et statut
    long countByRoleAndStatus(Role role, AccountStatus status);
}