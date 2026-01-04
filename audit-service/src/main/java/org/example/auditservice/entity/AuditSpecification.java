package org.example.auditservice.entity;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;


public class AuditSpecification {

    public static Specification<AuditLog> withFilters(
            String userId,
            String service,
            String status,
            String eventType,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (userId != null)
                predicates.add(cb.equal(root.get("userId"), userId));

            if (service != null)
                predicates.add(cb.equal(root.get("service"), service));

            if (status != null)
                predicates.add(cb.equal(root.get("status"), status));

            if (eventType != null)
                predicates.add(cb.equal(root.get("eventType"), eventType));

            if (from != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));

            if (to != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

