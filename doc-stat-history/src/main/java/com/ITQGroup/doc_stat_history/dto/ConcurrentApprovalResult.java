package com.ITQGroup.doc_stat_history.dto;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;

public record ConcurrentApprovalResult(
        int successCount,
        int conflictCount,
        int errorCount,
        DocumentStatus finalStatus
) {}