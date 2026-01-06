package org.example.auditservice.service;

import org.example.auditservice.entity.AuditLog;
import org.example.auditservice.entity.AuditSpecification;
import org.example.auditservice.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public Page<AuditLog> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<AuditLog> getByUser(String userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable);
    }

    public Page<AuditLog> getByEvent(String eventType, Pageable pageable) {
        return repository.findByEventType(eventType, pageable);
    }

    public AuditLog save(AuditLog log) {
        return repository.save(log);
    }



    public Page<AuditLog> search(
            String userId,
            String serviceName,
            String status,
            String eventType,
            Instant from,
            Instant to,
            Pageable pageable) {

        Specification<AuditLog> spec =
                AuditSpecification.withFilters(userId, serviceName, status, eventType, from, to);

        return repository.findAll(spec, pageable);
    }


    public Optional<AuditLog> getById(UUID id) {
        return repository.findById(id);
    }



}

