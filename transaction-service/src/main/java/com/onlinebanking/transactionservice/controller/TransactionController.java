package com.onlinebanking.transactionservice.controller;

import com.onlinebanking.transactionservice.model.Transaction;
import com.onlinebanking.transactionservice.service.TransactionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.onlinebanking.transactionservice.service.AuditProducerService;
import com.onlinebanking.transactionservice.service.TransactionService;
import com.onlinebanking.transactionservice.dto.AuditEventType;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    private AuditProducerService auditProducerService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Méthode utilitaire pour obtenir l'adresse IP du client
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @GetMapping
    public List<Transaction> getAllTransactions(HttpServletRequest request) {
        // Audit : consultation de toutes les transactions
        auditProducerService.sendAuditEvent(
                AuditEventType.TRANSACTION_LIST_VIEWED.getValue(),
                "SYSTEM", // ou l'ID utilisateur si authentifié
                "VIEW_ALL_TRANSACTIONS",
                getClientIp(request),
                "SUCCESS",
                Map.of("endpoint", "/transactions")
        );

        return transactionService.getAllTransactions();
    }

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction, HttpServletRequest request) {
        Transaction created = transactionService.createTransaction(transaction);

        // Audit : création d'une transaction
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transactionId", created.getId());
        metadata.put("accountNumber", created.getAccountNumber());
        metadata.put("amount", created.getAmount());
        metadata.put("type", created.getType());

        auditProducerService.sendAuditEvent(
                AuditEventType.TRANSACTION_CREATED.getValue(),
                created.getUsername(),
                "CREATE_TRANSACTION",
                getClientIp(request),
                "SUCCESS",
                metadata
        );

        return created;
    }

    @GetMapping("/user/{username}")
    public List<Transaction> getTransactionsByUsername(@PathVariable String username, HttpServletRequest request) {
        // Audit : consultation des transactions par utilisateur
        auditProducerService.sendAuditEvent(
                AuditEventType.TRANSACTION_VIEWED.getValue(),
                username,
                "VIEW_USER_TRANSACTIONS",
                getClientIp(request),
                "SUCCESS",
                Map.of("username", username)
        );

        return transactionService.getTransactionsByUsername(username);
    }

    @GetMapping("/account/{accountNumber}")
    public List<Transaction> getTransactionsByAccountNumber(@PathVariable String accountNumber, HttpServletRequest request) {
        // Audit : consultation des transactions par compte
        auditProducerService.sendAuditEvent(
                AuditEventType.TRANSACTION_VIEWED.getValue(),
                "UNKNOWN", // À remplacer par l'utilisateur authentifié
                "VIEW_ACCOUNT_TRANSACTIONS",
                getClientIp(request),
                "SUCCESS",
                Map.of("accountNumber", accountNumber)
        );

        return transactionService.getTransactionsByAccountNumber(accountNumber);
    }

    @GetMapping("/account/{accountNumber}/all")
    public List<Transaction> getAllTransactionsForAccount(@PathVariable String accountNumber, HttpServletRequest request) {
        // Audit : consultation de toutes les transactions d'un compte
        auditProducerService.sendAuditEvent(
                AuditEventType.ACCOUNT_TRANSACTIONS_VIEWED.getValue(),
                "UNKNOWN", // À remplacer par l'utilisateur authentifié
                "VIEW_ALL_ACCOUNT_TRANSACTIONS",
                getClientIp(request),
                "SUCCESS",
                Map.of("accountNumber", accountNumber)
        );

        return transactionService.getAllTransactionsForAccount(accountNumber);
    }

    @PostMapping("/transfer")
    public List<Transaction> createTransferTransactions(@RequestBody java.util.Map<String, Object> requestBody,
                                                        HttpServletRequest request) {

        Double amount;
        Object amountObj = requestBody.get("amount");
        if (amountObj instanceof Integer) {
            amount = ((Integer) amountObj).doubleValue();
        } else {
            amount = (Double) amountObj;
        }
        String fromAccountNumber = (String) requestBody.get("fromAccountNumber");
        String toAccountNumber = (String) requestBody.get("toAccountNumber");
        //Double amount = (Double) requestBody.get("amount");
        String fromUsername = (String) requestBody.get("fromUsername");
        String toUsername = (String) requestBody.get("toUsername");
        String description = (String) requestBody.get("description");

        List<Transaction> transactions = transactionService.createTransferTransactions(
                fromAccountNumber, toAccountNumber, amount, fromUsername, toUsername, description);

        // Audit : création d'un virement
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transferId", transactions.get(0).getTransferId());
        metadata.put("fromAccount", fromAccountNumber);
        metadata.put("toAccount", toAccountNumber);
        metadata.put("amount", amount);
        metadata.put("description", description);

        auditProducerService.sendAuditEvent(
                AuditEventType.TRANSFER_CREATED.getValue(),
                fromUsername,
                "CREATE_TRANSFER",
                getClientIp(request),
                "SUCCESS",
                metadata
        );

        // Audit supplémentaire pour le destinataire (optionnel)
        Map<String, Object> recipientMetadata = new HashMap<>(metadata);
        recipientMetadata.put("role", "RECIPIENT");

        auditProducerService.sendAuditEvent(
                AuditEventType.TRANSFER_INCOMING.getValue(),
                toUsername,
                "RECEIVE_TRANSFER",
                getClientIp(request),
                "SUCCESS",
                recipientMetadata
        );

        return transactions;
    }
}