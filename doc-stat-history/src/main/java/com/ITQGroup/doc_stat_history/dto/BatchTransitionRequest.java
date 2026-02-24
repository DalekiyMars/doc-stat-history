package com.ITQGroup.doc_stat_history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchTransitionRequest(
        @NotEmpty @Size(min = 1, max = 1000) List<Long> ids,
        @NotBlank @Size(max = 100) String initiator,
        String comment
) {}