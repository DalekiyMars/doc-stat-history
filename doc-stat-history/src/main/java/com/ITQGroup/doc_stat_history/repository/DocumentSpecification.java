package com.ITQGroup.doc_stat_history.repository;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.entity.Document;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DocumentSpecification {

    // Поиск по дате создания
    public static Specification<Document> withFilters(
            DocumentStatus status, String author,
            LocalDateTime from, LocalDateTime to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Objects.nonNull(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (Objects.nonNull(author) && !author.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("author")),
                        "%" + author.toLowerCase() + "%"));
            }
            if (Objects.nonNull(from)) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (Objects.nonNull(to)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}