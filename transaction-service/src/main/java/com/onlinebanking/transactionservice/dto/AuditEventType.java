package com.onlinebanking.transactionservice.dto;


public enum AuditEventType {

    // Événements de transaction
    TRANSACTION_CREATED("TRANSACTION_CREATED"),
    TRANSACTION_VIEWED("TRANSACTION_VIEWED"),
    TRANSACTION_LIST_VIEWED("TRANSACTION_LIST_VIEWED"),

    // Événements de virement
    TRANSFER_CREATED("TRANSFER_CREATED"),
    TRANSFER_OUTGOING("TRANSFER_OUTGOING"),
    TRANSFER_INCOMING("TRANSFER_INCOMING"),

    // Événements de compte
    ACCOUNT_TRANSACTIONS_VIEWED("ACCOUNT_TRANSACTIONS_VIEWED");

    private final String value;

    AuditEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
