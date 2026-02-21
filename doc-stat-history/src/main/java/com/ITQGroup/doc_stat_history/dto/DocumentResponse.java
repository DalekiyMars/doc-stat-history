package com.ITQGroup.doc_stat_history.dto;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        Long id,
        String uniqueNumber,
        String author,
        String docName,
        DocumentStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastUpdate,
        List<AuditResponse> audits
) {}