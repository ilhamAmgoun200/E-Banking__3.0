package com.onlinebanking.transactionservice.service;

import com.onlinebanking.transactionservice.model.Transaction;
import com.onlinebanking.transactionservice.repository.TransactionRepository;
import com.onlinebanking.transactionservice.dto.NotificationRequest;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final String notificationServiceUrl = "http://localhost:8087/notifications";

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

    public List<Transaction> getAllTransactionsForAccount(String accountNumber) {
        List<Transaction> regularTransactions = transactionRepository.findByAccountNumber(accountNumber);
        List<Transaction> fromTransactions = transactionRepository.findByFromAccountNumber(accountNumber);
        List<Transaction> toTransactions = transactionRepository.findByToAccountNumber(accountNumber);

        java.util.Set<Transaction> allTransactions = new java.util.LinkedHashSet<>(regularTransactions);
        allTransactions.addAll(fromTransactions);
        allTransactions.addAll(toTransactions);

        return new java.util.ArrayList<>(allTransactions);
    }

    // ---------------- Notification Helper ----------------
    private void sendNotification(String email, String phoneNumber, String message) {
        NotificationRequest req = new NotificationRequest();
        req.setEmail(email);
        req.setPhoneNumber(phoneNumber);
        req.setMessage(message);

        try {
            restTemplate.postForObject(notificationServiceUrl, req, Void.class);
        } catch (Exception e) {
            System.out.println(" Notification failed to send: " + e.getMessage());
        }
    }

    // ---------------- Transfer with Fraud Detection & Notifications ----------------
    public List<Transaction> createTransferTransactions(String fromAccountNumber, String toAccountNumber,
                                                        Double amount, String fromUsername, String toUsername,
                                                        String description) {
        String transferId = "TXN" + System.currentTimeMillis();

        // -------- Fraud detection placeholder --------
        boolean isFraud = false; // Replace with real fraudService.isFraudulent()
        String fraudReason = null;

        if (amount != null && amount > 10000) { // Example rule
            isFraud = true;
            fraudReason = "Transaction exceeds the limmit 10,000 ";
        }

        // -------- Create outgoing transaction --------
        Transaction outgoingTransaction = new Transaction();
        outgoingTransaction.setAccountNumber(fromAccountNumber);
        outgoingTransaction.setAmount(-amount);
        outgoingTransaction.setType("TRANSFER_OUT");
        outgoingTransaction.setUsername(fromUsername);
        outgoingTransaction.setTimestamp(java.time.LocalDateTime.now());
        outgoingTransaction.setToAccountNumber(toAccountNumber);
        outgoingTransaction.setFromAccountNumber(fromAccountNumber);
        outgoingTransaction.setTransferId(transferId);
        outgoingTransaction.setDescription(description != null ? description : "Transfer to " + toAccountNumber);

        // -------- Create incoming transaction --------
        Transaction incomingTransaction = new Transaction();
        incomingTransaction.setAccountNumber(toAccountNumber);
        incomingTransaction.setAmount(amount);
        incomingTransaction.setType("TRANSFER_IN");
        incomingTransaction.setUsername(toUsername);
        incomingTransaction.setTimestamp(java.time.LocalDateTime.now());
        incomingTransaction.setToAccountNumber(toAccountNumber);
        incomingTransaction.setFromAccountNumber(fromAccountNumber);
        incomingTransaction.setTransferId(transferId);
        incomingTransaction.setDescription(description != null ? description : "Transfer from " + fromAccountNumber);

        // -------- Set status based on fraud --------
        if (isFraud) {
            outgoingTransaction.setStatus("FAILED");
            outgoingTransaction.setFraudReason(fraudReason);

            incomingTransaction.setStatus("FAILED");
            incomingTransaction.setFraudReason(fraudReason);
        } else {
            outgoingTransaction.setStatus("SUCCESS");
            incomingTransaction.setStatus("SUCCESS");
        }

        // -------- Save transactions --------
        Transaction savedOutgoing = transactionRepository.save(outgoingTransaction);
        Transaction savedIncoming = transactionRepository.save(incomingTransaction);

        // -------- Send notifications --------
        String fromEmail = getUserEmailByUsername(fromUsername);
        String fromPhone = getUserPhoneByUsername(fromUsername);
        String toEmail = getUserEmailByUsername(toUsername);
        String toPhone = getUserPhoneByUsername(toUsername);

        if ("FAILED".equals(savedOutgoing.getStatus())) {
            sendNotification(fromEmail, fromPhone,
                    "Your transaction " + transferId + " FAILED: " + savedOutgoing.getFraudReason());
        } else {
            sendNotification(fromEmail, fromPhone,
                    "Your transaction " + transferId + " SUCCESS!");
        }

        sendNotification(toEmail, toPhone,
                "You received " + savedIncoming.getAmount() + " from " + fromAccountNumber +
                        ". Status: " + savedIncoming.getStatus());

        return List.of(savedOutgoing, savedIncoming);
    }


    private String getUserEmailByUsername(String username) {
        // TODO: Replace with REST call to user-service
        return username + "@gmail.com";
    }

    private String getUserPhoneByUsername(String username) {
        // TODO: Replace with REST call to user-service
        return "+212669926339";
    }
}
