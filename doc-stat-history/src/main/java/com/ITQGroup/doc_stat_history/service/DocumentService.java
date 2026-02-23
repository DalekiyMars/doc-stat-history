package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.CursorResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentSearchRequest;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.exception.DocumentNotFoundException;
import com.ITQGroup.doc_stat_history.mapper.DocumentMapper;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import com.ITQGroup.doc_stat_history.util.UniqueNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper mapper;
    private final UniqueNumberGenerator numberGenerator;

    // Создание документа в статусе DRAFT.
    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        Document doc = new Document()
                            .setAuthor(request.author())
                            .setDocName(request.docName())
                            .setStatus(DocumentStatus.DRAFT)
                            .setUniqueNumber(numberGenerator.generate());

        Document saved = documentRepository.save(doc);
        log.info("Document created: id={}, number={}", saved.getId(), saved.getUniqueNumber());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        log.debug("Document found: id={}, number={}", id, doc.toString());
        return mapper.toResponse(doc);
    }

    // Получение одного документа вместе с историей аудита.
    @Transactional(readOnly = true)
    public DocumentResponse getWithAudits(Long id) {
        return documentRepository.findByIdWithAudits(id)
                .map(mapper::toResponseWithAudits)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    // Пакетное получение документов по списку id с пагинацией и сортировкой.
    @Transactional(readOnly = true)
    public Slice<DocumentResponse> getByIds(List<Long> ids, Pageable pageable) {
        return documentRepository.findByIdIn(ids, pageable)
                .map(mapper::toResponse);
    }

    // Поиск документов с динамическими фильтрами (по createdAt)
    @Transactional(readOnly = true)
    public CursorResponse<DocumentResponse> search(DocumentSearchRequest req,
                                                   int limit,
                                                   Long cursor) {
        // Передаём status как String — нативный запрос не знает про enum
        String statusStr = req.status() != null ? req.status().name() : null;

        List<Document> docs = documentRepository.searchWithCursor(
                statusStr,
                req.author(),
                req.from(),
                req.to(),
                cursor,
                limit
        );

        // Маппим в DTO
        List<DocumentResponse> content = docs.stream()
                .map(mapper::toResponse)
                .toList();

        // id последнего элемента становится курсором следующей страницы
        Long nextCursor = content.isEmpty()
                ? null
                : docs.getLast().getId();

        return CursorResponse.of(content, limit, nextCursor);
    }
}