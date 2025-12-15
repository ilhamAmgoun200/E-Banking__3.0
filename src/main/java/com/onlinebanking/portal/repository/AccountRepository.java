package com.onlinebanking.portal.repository;

import com.onlinebanking.portal.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {}