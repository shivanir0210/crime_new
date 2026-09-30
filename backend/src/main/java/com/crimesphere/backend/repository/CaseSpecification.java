package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Case;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseSpecification {

    public static Specification<Case> withFilters(
            UUID stationId,
            String search,
            String status,
            String priority,
            String caseType
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Station Isolation
            if (stationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("station").get("id"), stationId));
            }

            // 2. Search Keyword across caseNumber, title, and description
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate caseNumPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("caseNumber")), searchPattern);
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(caseNumPredicate, titlePredicate, descPredicate));
            }

            // 3. Status Filter
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status.trim().toUpperCase()));
            }

            // 4. Priority Filter
            if (priority != null && !priority.trim().isEmpty() && !priority.equalsIgnoreCase("ALL")) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority.trim().toUpperCase()));
            }

            // 5. Case Type Filter
            if (caseType != null && !caseType.trim().isEmpty() && !caseType.equalsIgnoreCase("ALL")) {
                predicates.add(criteriaBuilder.equal(root.get("caseType"), caseType.trim().toUpperCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
