package com.ITQGroup.doc_stat_history.mapper;

import com.ITQGroup.doc_stat_history.dto.AuditResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import com.ITQGroup.doc_stat_history.entity.Audit;
import com.ITQGroup.doc_stat_history.entity.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentMapper {

    // Маппинг без истории
    public DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
                doc.getId(), doc.getUniqueNumber(), doc.getAuthor(),
                doc.getDocName(), doc.getStatus(),
                doc.getCreatedAt(), doc.getLastUpdate(), null
        );
    }

    // Маппинг с историей
    public DocumentResponse toResponseWithAudits(Document doc) {
        List<AuditResponse> audits = doc.getAudits().stream()
                .map(this::toAuditResponse)
                .toList();
        return new DocumentResponse(
                doc.getId(), doc.getUniqueNumber(), doc.getAuthor(),
                doc.getDocName(), doc.getStatus(),
                doc.getCreatedAt(), doc.getLastUpdate(), audits
        );
    }

    public AuditResponse toAuditResponse(Audit audit) {
        return new AuditResponse(
                audit.getId(), audit.getActionAuthor(),
                audit.getActionTime(), audit.getActionType(), audit.getComment()
        );
    }
}