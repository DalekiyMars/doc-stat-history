package com.ITQGroup.doc_stat_history.dto;

import com.ITQGroup.doc_stat_history.common.ActionType;

import java.time.LocalDateTime;

public record AuditResponse(
        Long id,
        String actionAuthor,
        LocalDateTime actionTime,
        ActionType actionType,
        String comment
) {}