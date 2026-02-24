package com.ITQGroup.doc_stat_history.dto;


import com.ITQGroup.doc_stat_history.common.DocumentStatus;

import java.time.LocalDateTime;

// Поиск ведётся по дате создания (createdAt)
public record DocumentSearchRequest(
        DocumentStatus status,
        String author,
        LocalDateTime from,
        LocalDateTime to
) {}