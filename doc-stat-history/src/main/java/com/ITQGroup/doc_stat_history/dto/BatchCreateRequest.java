package com.ITQGroup.doc_stat_history.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchCreateRequest(
        @NotEmpty @Size(min = 1, max = 1000) List<CreateDocumentRequest> documents
) {}