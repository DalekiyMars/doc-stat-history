package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.ActionType;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.entity.ApprovalRegistry;
import com.ITQGroup.doc_stat_history.entity.Audit;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.repository.ApprovalRegistryRepository;
import com.ITQGroup.doc_stat_history.repository.AuditRepository;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Выделен в отдельный бин специально, чтобы @Transactional(REQUIRES_NEW)
 * работал через Spring-прокси — вызовы приходят извне класса (из TransitionService),
 * а не через this, поэтому прокси перехватывает их корректно.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransitionExecutor {

    private final DocumentRepository documentRepository;
    private final AuditRepository auditRepository;
    private final ApprovalRegistryRepository registryRepository;

    // REQUIRES_NEW — собственная транзакция на каждый документ.
    // Если упадёт, откатится только этот один документ, остальные не затронуты.
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
                    saveAudit(doc, initiator, ActionType.SUBMIT, comment);
                    documentRepository.save(doc);
                    log.info("Document {} submitted by {}", id, initiator);
                    return new TransitionResult(id, ResultStatus.SUCCESS, null);
                })
                .orElseGet(() -> {
                    log.warn("Submit: document {} not found", id);
                    return new TransitionResult(id, ResultStatus.NOT_FOUND, "Document not found");
                });
    }

    // REQUIRES_NEW + rollbackFor — при любом исключении
    // вся транзакция откатывается: документ остаётся в SUBMITTED, аудит не пишется.
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TransitionResult approveOne(Long id, String initiator, String comment) {
        Document doc = documentRepository.findByIdForUpdate(id).orElse(null);

        if (doc == null) {
            return new TransitionResult(id, ResultStatus.NOT_FOUND, "Document not found");
        }
        if (doc.getStatus() != DocumentStatus.SUBMITTED) {
            return new TransitionResult(id, ResultStatus.CONFLICT,
                    "Expected SUBMITTED, got " + doc.getStatus());
        }

        doc.setStatus(DocumentStatus.APPROVED);
        saveAudit(doc, initiator, ActionType.APPROVE, comment);
        documentRepository.save(doc);

        // Запись в реестр. При исключении Spring откатит всю REQUIRES_NEW транзакцию —
        // документ вернётся в SUBMITTED, аудит тоже не сохранится.
        try {
            ApprovalRegistry registry = new ApprovalRegistry();
            registry.setDocument(doc);
            registry.setInitiator(initiator);
            registry.setApprovedAt(LocalDateTime.now());
            registryRepository.save(registry);
            log.info("Document {} approved by {}", id, initiator);
            return new TransitionResult(id, ResultStatus.SUCCESS, null);
        } catch (Exception e) {
            log.error("Registry save failed for document {}: {}", id, e.getMessage());
            throw new RuntimeException("Registry error: " + e.getMessage(), e);
        }
    }

    private void saveAudit(Document doc, String initiator, ActionType actionType, String comment) {
        Audit audit = new Audit();
        audit.setDocument(doc);
        audit.setActionAuthor(initiator);
        audit.setActionTime(LocalDateTime.now());
        audit.setActionType(actionType);
        audit.setComment(comment);
        auditRepository.save(audit);
    }
}