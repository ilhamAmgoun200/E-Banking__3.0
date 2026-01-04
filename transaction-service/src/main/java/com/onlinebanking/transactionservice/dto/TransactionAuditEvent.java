package com.onlinebanking.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public class TransactionAuditEvent {

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("service")
    private String service;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("status")
    private String status;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("metadata")
    private String metadata;

    // Constructeurs
    public TransactionAuditEvent() {}

    public TransactionAuditEvent(String eventType, String service, String userId, String action,
                                 String ipAddress, String status, Instant timestamp, String metadata) {
        this.eventType = eventType;
        this.service = service;
        this.userId = userId;
        this.action = action;
        this.ipAddress = ipAddress;
        this.status = status;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    // Getters et Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    @Override
    public String toString() {
        return "TransactionAuditEvent{" +
                "eventType='" + eventType + '\'' +
                ", service='" + service + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", metadata='" + metadata + '\'' +
                '}';
    }
}
