package com.ITQGroup.doc_stat_history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank @Size(max = 100) String author,
        @NotBlank @Size(max = 200) String docName,
        @NotBlank @Size(max = 100) String initiator
) {}