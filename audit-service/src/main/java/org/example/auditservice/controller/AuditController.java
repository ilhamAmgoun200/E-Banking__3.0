package org.example.auditservice.controller;

import org.example.auditservice.entity.AuditLog;
import org.example.auditservice.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> all(Pageable pageable) {
        Page<AuditLog> page = service.getAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("size", page.getSize());
        response.put("number", page.getNumber());
        response.put("first", page.isFirst());
        response.put("last", page.isLast());

        return response;
    }

    @GetMapping("/user/{userId}")
    public Map<String, Object> byUser(@PathVariable String userId, Pageable pageable) {
        Page<AuditLog> page = service.getByUser(userId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());

        return response;
    }

    @GetMapping("/event/{type}")
    public Map<String, Object> byEvent(@PathVariable String type, Pageable pageable) {
        Page<AuditLog> page = service.getByEvent(type, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());

        return response;
    }

    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {

        Page<AuditLog> page = service.search(userId, serviceName, status, eventType, from, to, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("size", page.getSize());
        response.put("number", page.getNumber());
        response.put("first", page.isFirst());
        response.put("last", page.isLast());

        return response;
    }

    /**
     * Endpoint pour les statistiques d'audit
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats(Pageable pageable) {
        Page<AuditLog> allLogs = service.getAll(pageable);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", allLogs.getTotalElements());

        // Pour compter les succès/échecs
        Page<AuditLog> successLogs = service.search(null, null, "SUCCESS", null, null, null, pageable);
        Page<AuditLog> failedLogs = service.search(null, null, "FAILED", null, null, null, pageable);

        stats.put("successEvents", successLogs.getTotalElements());
        stats.put("failedEvents", failedLogs.getTotalElements());

        // Compter services uniques
        long uniqueServices = allLogs.getContent().stream()
                .map(AuditLog::getService)
                .filter(serviceName -> serviceName != null && !serviceName.isEmpty())
                .distinct()
                .count();

        stats.put("uniqueServices", uniqueServices);

        return stats;
    }


    @GetMapping("/{id}/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata(@PathVariable UUID id) {
        return service.getById(id)
                .map(log -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("metadata", log.getMetadata()); // assuming AuditLog has getMetadata()
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping("/test")
    public AuditLog test(@RequestBody AuditLog log) {
        return service.save(log);
    }
}