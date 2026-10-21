package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.TenantAuditFilterCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TenantAuditEventSpecifications {

    private TenantAuditEventSpecifications() {
    }

    public static Specification<TenantAuditEventEntity> fromCriteria(TenantAuditFilterCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.startDate()));
            }

            if (criteria.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.endDate()));
            }

            if (criteria.actor() != null && !criteria.actor().isBlank()) {
                String actorPattern = "%" + criteria.actor().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorEmail")), actorPattern),
                        cb.like(cb.lower(root.get("actorSubject")), actorPattern)
                ));
            }

            if (criteria.resourceType() != null && !criteria.resourceType().isBlank() && !"all".equalsIgnoreCase(criteria.resourceType())) {
                predicates.add(cb.equal(cb.upper(root.get("resourceType")), criteria.resourceType().trim().toUpperCase()));
            }

            if (criteria.action() != null && !criteria.action().isBlank() && !"all".equalsIgnoreCase(criteria.action())) {
                String action = criteria.action().trim().toUpperCase();
                predicates.add(cb.or(
                        cb.equal(cb.upper(root.get("eventType")), action),
                        cb.like(cb.upper(root.get("eventType")), "%" + action + "%")
                ));
            }

            if (criteria.module() != null && !criteria.module().isBlank() && !"all".equalsIgnoreCase(criteria.module())) {
                String mod = criteria.module().trim().toUpperCase();
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("resourceType")), "%" + mod + "%"),
                        cb.like(cb.upper(root.get("eventType")), "%" + mod + "%")
                ));
            }

            if (criteria.search() != null && !criteria.search().isBlank()) {
                String searchPattern = "%" + criteria.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorEmail")), searchPattern),
                        cb.like(cb.lower(root.get("actorSubject")), searchPattern),
                        cb.like(cb.lower(root.get("eventType")), searchPattern),
                        cb.like(cb.lower(root.get("resourceType")), searchPattern),
                        cb.like(cb.lower(root.get("resourceId")), searchPattern),
                        cb.like(cb.lower(root.get("eventDetails")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
