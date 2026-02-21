package com.ITQGroup.doc_stat_history.repository;

import com.ITQGroup.doc_stat_history.entity.ApprovalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {

    Optional<ApprovalRegistry> findByDocumentId(Long documentId);
    boolean existsByDocumentId(Long documentId);
}