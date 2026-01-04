package com.onlinebanking.transactionservice.fraud;

import com.onlinebanking.transactionservice.model.Transaction;
import org.springframework.stereotype.Service;

@Service
public class FraudService {

    // Example basic fraud rules
    public boolean isFraudulent(Transaction transaction) {
        // 1️⃣ Block transfers > 10,000 for testing
        if (Math.abs(transaction.getAmount()) > 10000) {
            return true;
        }
        // 2️⃣ Block transfers between same account (suspicious)
        if (transaction.getFromAccountNumber().equals(transaction.getToAccountNumber())) {
            return true;
        }
        // 3️⃣ You can add more rules: frequency, location, blacklist accounts, etc.
        return false;
    }

    public String getFraudReason(Transaction transaction) {
        if (Math.abs(transaction.getAmount()) > 10000) {
            return "Transaction amount exceeds limit";
        }
        if (transaction.getFromAccountNumber().equals(transaction.getToAccountNumber())) {
            return "Transfer to same account is suspicious";
        }
        return "";
    }
}
