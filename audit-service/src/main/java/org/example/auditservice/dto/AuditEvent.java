package org.example.auditservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;


public class AuditEvent {

    private String eventType;
    private String service;
    private String userId;
    private String action;
    private String ipAddress;
    private String status;
    private Instant timestamp;
    private String metadata;

    public String getEventType() {
        return eventType;
    }

    public String getService() {
        return service;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMetadata() {
        return metadata;
    }
}

