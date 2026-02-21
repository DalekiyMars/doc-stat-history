package com.ITQGroup.doc_stat_history.repository;

import com.ITQGroup.doc_stat_history.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long>,
        JpaSpecificationExecutor<Document> {

    // Для пакетного получения документов по id с пагинацией
    Page<Document> findByIdIn(List<Long> ids, Pageable pageable);

    // Получение документа с историей — одним запросом через JOIN FETCH
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.audits WHERE d.id = :id")
    Optional<Document> findByIdWithAudits(@Param("id") Long id);

    // Используется в методе approve для предотвращения двойного утверждения
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT d.id FROM entity_tables.documents d WHERE d.status = :status LIMIT :limit",
            nativeQuery = true)
    List<Long> findIdsByStatus(@Param("status") String status, @Param("limit") int limit);
}