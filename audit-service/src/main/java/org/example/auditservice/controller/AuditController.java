package org.example.auditservice.controller;

import org.example.auditservice.entity.AuditLog;
import org.example.auditservice.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AuditLog> all(Pageable pageable) {
        return service.getAll(pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<AuditLog> byUser(@PathVariable String userId, Pageable pageable) {
        return service.getByUser(userId, pageable);
    }

    @GetMapping("/event/{type}")
    public Page<AuditLog> byEvent(@PathVariable String type, Pageable pageable) {
        return service.getByEvent(type, pageable);
    }


    @GetMapping("/search")
    public Page<AuditLog> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {

        return service.search(userId, serviceName, status, eventType, from, to, pageable);
    }



    @PostMapping("/test")
    public AuditLog test(@RequestBody AuditLog log) {
        return service.save(log);
    }

}

