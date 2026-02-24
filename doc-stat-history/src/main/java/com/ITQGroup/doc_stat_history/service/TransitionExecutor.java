package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.ActionType;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.entity.ApprovalRegistry;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.repository.ApprovalRegistryRepository;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransitionExecutor {

    private final DocumentRepository documentRepository;
    private final ApprovalRegistryRepository registryRepository;
    private final Clock clock;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransitionResult submitOne(Long id, String initiator, String comment) {
        return documentRepository.findByIdForUpdate(id)
                .map(doc -> {
                    if (doc.getStatus() != DocumentStatus.DRAFT) {
                        log.warn("Submit conflict: doc={} status={}", id, doc.getStatus());
                        return new TransitionResult(id, ResultStatus.CONFLICT,
                                "Expected DRAFT, got " + doc.getStatus());
                    }
                    doc.setStatus(DocumentStatus.SUBMITTED);
                    documentRepository.save(doc);
                    auditService.saveAudit(doc, initiator, ActionType.SUBMIT, comment);
                    log.info("Document {} submitted by {}", id, initiator);
                    return new TransitionResult(id, ResultStatus.SUCCESS, null);
                })
                .orElseGet(() -> {
                    log.warn("Submit: document {} not found", id);
                    return new TransitionResult(id, ResultStatus.NOT_FOUND, "Document not found");
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TransitionResult approveOne(Long id, String initiator, String comment) {
        Document doc = documentRepository.findByIdForUpdate(id).orElse(null);

        if (Objects.isNull(doc)) {
            return new TransitionResult(id, ResultStatus.NOT_FOUND, "Document not found");
        }
        if (!Objects.equals(doc.getStatus(), DocumentStatus.SUBMITTED)) {
            return new TransitionResult(id, ResultStatus.CONFLICT,
                    "Expected SUBMITTED, got " + doc.getStatus());
        }

        doc.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(doc);
        auditService.saveAudit(doc, initiator, ActionType.APPROVE, comment);

        // Запись в реестр. При исключении Spring откатит всю транзакцию
        // документ вернётся в SUBMITTED, аудит тоже не сохранится.
        try {
            ApprovalRegistry registry = new ApprovalRegistry()
                                            .setDocument(doc)
                                            .setInitiator(initiator)
                                            .setApprovedAt(LocalDateTime.now(clock));
            registryRepository.save(registry);
            log.info("Document {} approved by {}", id, initiator);
            return new TransitionResult(id, ResultStatus.SUCCESS, null);
        } catch (Exception e) {
            log.error("Registry save failed for document {}: {}", id, e.getMessage());
            throw new RuntimeException("Registry error: " + e.getMessage(), e);
        }
    }
}