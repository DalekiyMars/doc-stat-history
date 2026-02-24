package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.ActionType;
import com.ITQGroup.doc_stat_history.entity.Audit;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRepository auditRepository;

    public void saveAudit(Document doc, String author, ActionType actionType, String comment) {
        Audit audit = new Audit()
                .setDocument(doc)
                .setActionAuthor(author)
                .setActionType(actionType)
                .setComment(comment);
        auditRepository.save(audit);
    }

    public void saveAll(List<Audit> audits) {
        auditRepository.saveAll(audits);  // Batch save audits
    }
}
