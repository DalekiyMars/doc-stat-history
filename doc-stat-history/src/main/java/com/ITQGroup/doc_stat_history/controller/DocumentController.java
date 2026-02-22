package com.ITQGroup.doc_stat_history.controller;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentSearchRequest;
import com.ITQGroup.doc_stat_history.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // Создание документа в статусе DRAFT.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.create(request);
    }

    // Получение одного документа. Флаг withHistory переключает режим
    @GetMapping("/{id}")
    public DocumentResponse getById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean withHistory) {
        return withHistory
                ? documentService.getWithAudits(id)
                : documentService.getById(id);
    }

    // Пакетное получение по списку id с пагинацией и сортировкой.
    @GetMapping("/batch")
    public Page<DocumentResponse> getByIds(
            @RequestParam @NotEmpty @Size(max = 1000) List<Long> ids,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return documentService.getByIds(ids, pageable);
    }

    // Поиск с динамическими фильтрами. Все параметры опциональны.
    // Период фильтруется по createdAt
    @GetMapping("/search")
    public Window<DocumentResponse> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) Long scrollId) {

        // scrollId == null → первая страница; иначе → продолжение с позиции scrollId
        ScrollPosition position = scrollId == null
                ? ScrollPosition.keyset()
                : ScrollPosition.forward(java.util.Map.of("id", scrollId));

        var searchRequest = new DocumentSearchRequest(status, author, from, to);
        return documentService.search(searchRequest, limit, position);
    }
}