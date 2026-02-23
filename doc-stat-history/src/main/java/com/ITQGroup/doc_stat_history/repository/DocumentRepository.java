package com.ITQGroup.doc_stat_history.repository;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Slice<Document> findByIdIn(List<Long> ids, Pageable pageable);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.audits WHERE d.id = :id")
    Optional<Document> findByIdWithAudits(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT d.id FROM entity_tables.documents d " +
            "WHERE d.status = :status LIMIT :limit", nativeQuery = true)
    List<Long> findIdsByStatus(@Param("status") String status, @Param("limit") int limit);

    long countByStatus(DocumentStatus status);

    // Вызов PostgreSQL-функции через SELECT * FROM function().
    // nativeQuery = true — Spring Data передаёт запрос напрямую в JDBC без разбора JPQL.
    // Результат маппится на сущность Document по совпадению имён колонок.
    @Query(value = """
            SELECT * FROM entity_tables.search_documents(
                CAST(:status AS VARCHAR),
                :author,
                CAST(:from   AS TIMESTAMP),
                CAST(:to     AS TIMESTAMP),
                :cursor,
                :limit
            )
            """, nativeQuery = true)
    List<Document> searchWithCursor(
            @Param("status") String status,
            @Param("author") String author,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to,
            @Param("cursor") Long cursor,
            @Param("limit")  int limit
    );
}