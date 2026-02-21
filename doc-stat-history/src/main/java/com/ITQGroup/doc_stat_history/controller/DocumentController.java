package com.ITQGroup.doc_stat_history.controller;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentSearchRequest;
import com.ITQGroup.doc_stat_history.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // POST /api/v1/documents
    // Создание документа в статусе DRAFT.
    // @Valid запускает проверку полей CreateDocumentRequest (NotBlank, Size).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.create(request);
    }

    // GET /api/v1/documents/{id}?withHistory=true
    // Получение одного документа. Флаг withHistory переключает режим —
    // одним параметром покрываем оба сценария из задания без дублирования эндпоинта.
    @GetMapping("/{id}")
    public DocumentResponse getById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean withHistory) {
        return withHistory
                ? documentService.getWithAudits(id)
                : documentService.getById(id);
    }

    // GET /api/v1/documents/batch?ids=1,2,3&page=0&size=20&sort=createdAt,desc
    // Пакетное получение по списку id с пагинацией и сортировкой.
    // @PageableDefault задаёт значения по умолчанию, если клиент не передал параметры.
    // @NotEmpty + @Size — валидация списка id прямо на параметре запроса.
    @GetMapping("/batch")
    public Page<DocumentResponse> getByIds(
            @RequestParam @NotEmpty @Size(max = 1000) List<Long> ids,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return documentService.getByIds(ids, pageable);
    }

    // GET /api/v1/documents/search?status=DRAFT&author=Ivan&from=...&to=...&page=0&size=20
    // Поиск с динамическими фильтрами. Все параметры опциональны.
    // Период фильтруется по createdAt
    @GetMapping("/search")
    public Page<DocumentResponse> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        var searchRequest = new DocumentSearchRequest(status, author, from, to);
        return documentService.search(searchRequest, pageable);
    }
}