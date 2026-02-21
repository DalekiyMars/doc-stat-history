package com.ITQGroup.doc_stat_history.dto;

import com.ITQGroup.doc_stat_history.common.ResultStatus;

public record TransitionResult(
        Long documentId,
        ResultStatus result,
        String message
) {}