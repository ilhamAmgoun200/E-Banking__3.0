package com.onlinebanking.transactionservice.fraud;

import com.onlinebanking.transactionservice.model.Transaction;
import org.springframework.stereotype.Service;

@Service
public class FraudService {

    public boolean isFraudulent(Transaction transaction) {
        if (Math.abs(transaction.getAmount()) > 10000) {
            return true;
        }
        if (transaction.getFromAccountNumber().equals(transaction.getToAccountNumber())) {
            return true;
        }
        return false;
    }

    public String getFraudReason(Transaction transaction) {
        if (Math.abs(transaction.getAmount()) > 10000) {
            return "Transaction exceeds limit of 10,000";
        }
        if (transaction.getFromAccountNumber().equals(transaction.getToAccountNumber())) {
            return "Transfer to same account action is suspicious";
        }
        return "";
    }
}
