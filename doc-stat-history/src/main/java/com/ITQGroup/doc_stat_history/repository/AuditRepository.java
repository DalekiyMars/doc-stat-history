package com.ITQGroup.doc_stat_history.repository;

import com.ITQGroup.doc_stat_history.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, Long> {
}