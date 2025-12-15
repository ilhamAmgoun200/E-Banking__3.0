package com.onlinebanking.accountservice.repository;

import com.onlinebanking.accountservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUsername(String username);
    Account findByAccountNumber(String accountNumber);
}
