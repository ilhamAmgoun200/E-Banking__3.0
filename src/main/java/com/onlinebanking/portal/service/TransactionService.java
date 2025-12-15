package com.onlinebanking.portal.service;

import com.onlinebanking.portal.model.Account;
import com.onlinebanking.portal.model.Transaction;
import com.onlinebanking.portal.repository.AccountRepository;
import com.onlinebanking.portal.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional
    public Transaction createTransaction(Long accountId, double amount, String type) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if ("WITHDRAW".equalsIgnoreCase(type)) {
            if (account.getBalance() < amount) {
                throw new RuntimeException("Insufficient balance! Transaction rolled back.");
            }
            account.setBalance(account.getBalance() - amount);
        } else if ("DEPOSIT".equalsIgnoreCase(type)) {
            account.setBalance(account.getBalance() + amount);
        } else {
            throw new RuntimeException("Invalid transaction type! Must be DEPOSIT or WITHDRAW.");
        }
        Transaction txn = new Transaction();
        txn.setAmount(amount);
        txn.setType(type.toUpperCase());
        txn.setTimestamp(LocalDateTime.now());
        txn.setAccount(account);
        accountRepository.save(account);
        return transactionRepository.save(txn);
    }
}