package org.example.auditservice.service;

import org.example.auditservice.dto.AuditEvent;
import org.example.auditservice.entity.AuditLog;
import org.example.auditservice.repository.AuditLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);
    private final AuditLogRepository repository;

    // PLUS BESOIN D'ObjectMapper !
    public AuditConsumer(AuditLogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "audit-events", groupId = "audit-group")
    public void consume(AuditEvent event) {
        log.info("✅ RECEIVED AUDIT EVENT: {} from {}", event.getEventType(), event.getService());

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(event.getEventType());
            auditLog.setService(event.getService());
            auditLog.setUserId(event.getUserId());
            auditLog.setAction(event.getAction());
            auditLog.setIpAddress(event.getIpAddress());
            auditLog.setStatus(event.getStatus());
            auditLog.setTimestamp(event.getTimestamp());

            // DIRECTEMENT, pas de conversion !
            auditLog.setMetadata(event.getMetadata() != null ? event.getMetadata() : "{}");

            repository.save(auditLog);
            log.info("✅ Audit log saved. ID: {}, User: {}",
                    auditLog.getId(), event.getUserId());

        } catch (Exception e) {
            log.error("❌ Error saving audit log: {}", e.getMessage(), e);
        }
    }
}

/*@Service
public class AuditConsumer {

    private final AuditLogRepository repository;

    public AuditConsumer(AuditLogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "audit-events", groupId = "audit-group")
    public void consume(AuditEvent event) {

        System.out.println("🔥 RECEIVED EVENT: " + event);

        AuditLog log = new AuditLog();
        log.setEventType(event.getEventType());
        log.setService(event.getService());
        log.setUserId(event.getUserId());
        log.setAction(event.getAction());
        log.setIpAddress(event.getIpAddress());
        log.setStatus(event.getStatus());
        log.setTimestamp(event.getTimestamp());
        log.setMetadata(event.getMetadata().toString());

        repository.save(log);
    }
}*/

