package com.onlinebanking.transactionservice.service;

import com.onlinebanking.transactionservice.model.Transaction;
import com.onlinebanking.transactionservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsByUsername(String username) {
        return transactionRepository.findByUsername(username);
    }

    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }
    
    public List<Transaction> createTransferTransactions(String fromAccountNumber, String toAccountNumber, 
                                                       Double amount, String fromUsername, String toUsername, String description) {
        String transferId = "TXN" + System.currentTimeMillis();
        
        // Create outgoing transaction
        Transaction outgoingTransaction = new Transaction();
        outgoingTransaction.setAccountNumber(fromAccountNumber);
        outgoingTransaction.setAmount(-amount); // Negative for outgoing
        outgoingTransaction.setType("TRANSFER_OUT");
        outgoingTransaction.setUsername(fromUsername);
        outgoingTransaction.setTimestamp(java.time.LocalDateTime.now());
        outgoingTransaction.setToAccountNumber(toAccountNumber);
        outgoingTransaction.setFromAccountNumber(fromAccountNumber);
        outgoingTransaction.setTransferId(transferId);
        outgoingTransaction.setDescription(description != null ? description : "Transfer to " + toAccountNumber);
        
        // Create incoming transaction
        Transaction incomingTransaction = new Transaction();
        incomingTransaction.setAccountNumber(toAccountNumber);
        incomingTransaction.setAmount(amount); // Positive for incoming
        incomingTransaction.setType("TRANSFER_IN");
        incomingTransaction.setUsername(toUsername);
        incomingTransaction.setTimestamp(java.time.LocalDateTime.now());
        incomingTransaction.setToAccountNumber(toAccountNumber);
        incomingTransaction.setFromAccountNumber(fromAccountNumber);
        incomingTransaction.setTransferId(transferId);
        incomingTransaction.setDescription(description != null ? description : "Transfer from " + fromAccountNumber);
        
        // Save both transactions
        Transaction savedOutgoing = transactionRepository.save(outgoingTransaction);
        Transaction savedIncoming = transactionRepository.save(incomingTransaction);
        
        return List.of(savedOutgoing, savedIncoming);
    }
    
    public List<Transaction> getAllTransactionsForAccount(String accountNumber) {
        List<Transaction> regularTransactions = transactionRepository.findByAccountNumber(accountNumber);
        List<Transaction> fromTransactions = transactionRepository.findByFromAccountNumber(accountNumber);
        List<Transaction> toTransactions = transactionRepository.findByToAccountNumber(accountNumber);
        
        // Combine all transactions and remove duplicates
        java.util.Set<Transaction> allTransactions = new java.util.LinkedHashSet<>(regularTransactions);
        allTransactions.addAll(fromTransactions);
        allTransactions.addAll(toTransactions);
        
        return new java.util.ArrayList<>(allTransactions);
    }
}
