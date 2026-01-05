package org.example.auditservice.service;

import org.example.auditservice.dto.AuditEvent;
import org.example.auditservice.entity.AuditLog;
import org.example.auditservice.repository.AuditLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    // Injection via constructeur
    public AuditConsumer(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "audit-events", groupId = "audit-group")
    public void consume(String message) {
        log.info("📩 Raw message received: {}", message.substring(0, Math.min(100, message.length())) + "...");

        try {
            // Parse le JSON
            AuditEvent event = objectMapper.readValue(message, AuditEvent.class);

            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(event.getEventType());
            auditLog.setService(event.getService());
            auditLog.setUserId(event.getUserId());
            auditLog.setAction(event.getAction());
            auditLog.setIpAddress(event.getIpAddress());
            auditLog.setStatus(event.getStatus());
            auditLog.setTimestamp(event.getTimestamp());
            auditLog.setMetadata(event.getMetadata() != null ? event.getMetadata() : "{}");

            repository.save(auditLog);
            log.info("✅ Audit log saved. ID: {}, User: {}, Event: {}",
                    auditLog.getId(), event.getUserId(), event.getEventType());

        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse audit event. Error: {}", e.getMessage());
            log.debug("Full message that failed: {}", message);
        } catch (Exception e) {
            log.error("❌ Error saving audit log: {}", e.getMessage(), e);
        }
    }
}