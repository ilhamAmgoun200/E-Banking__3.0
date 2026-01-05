package com.onlinebanking.transactionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebanking.transactionservice.dto.TransactionAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditProducerService {

    private static final Logger log = LoggerFactory.getLogger(AuditProducerService.class);
    private static final String AUDIT_TOPIC = "audit-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Envoie un événement d'audit vers Kafka
     */
    public void sendAuditEvent(TransactionAuditEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(AUDIT_TOPIC, eventJson);
            log.info("✅ Audit event sent: {} for user: {}", event.getEventType(), event.getUserId());
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize audit event: {}", event, e);
        }
    }

    /**
     * Méthode utilitaire pour créer et envoyer un événement d'audit
     */
    public void sendAuditEvent(String eventType, String userId, String action,
                               String ipAddress, String status, Map<String, Object> metadata) {

        TransactionAuditEvent event = new TransactionAuditEvent(
                eventType,
                "transaction-service", // Nom du service
                userId,
                action,
                ipAddress,
                status,
                Instant.now(),
                convertMetadataToString(metadata)
        );

        sendAuditEvent(event);
    }

    /**
     * Convertit les métadonnées Map en String JSON
     */
    private String convertMetadataToString(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize metadata", e);
            return "{}";
        }
    }
}
