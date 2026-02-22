package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentSearchRequest;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.exception.DocumentNotFoundException;
import com.ITQGroup.doc_stat_history.mapper.DocumentMapper;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import com.ITQGroup.doc_stat_history.repository.DocumentSpecification;
import com.ITQGroup.doc_stat_history.util.UniqueNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
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
    public Page<DocumentResponse> getByIds(List<Long> ids, Pageable pageable) {
        return documentRepository.findByIdIn(ids, pageable)
                .map(mapper::toResponse);
    }

    // Поиск документов с динамическими фильтрами (по createdAt)
    @Transactional(readOnly = true)
    public Window<DocumentResponse> search(DocumentSearchRequest req,
                                           int limit,
                                           ScrollPosition scrollPosition) {
        var spec = DocumentSpecification.withFilters(
                req.status(), req.author(), req.from(), req.to());
        return documentRepository.scrollBySpec(spec, limit, scrollPosition)
                .map(mapper::toResponse);
    }
}