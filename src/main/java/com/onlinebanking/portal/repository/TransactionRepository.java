package com.onlinebanking.portal.repository;

import com.onlinebanking.portal.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {}