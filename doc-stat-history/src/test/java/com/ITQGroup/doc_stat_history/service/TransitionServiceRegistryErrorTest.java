package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.DocStatHistoryApplication;
import com.ITQGroup.doc_stat_history.TestcontainersConfiguration;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.entity.ApprovalRegistry;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.repository.ApprovalRegistryRepository;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(classes = DocStatHistoryApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers
class TransitionServiceRegistryErrorTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private TransitionExecutor transitionExecutor;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TransitionService transitionService;

    @MockitoBean
    private ApprovalRegistryRepository registryRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    void approveOne_registryError_shouldRollback() {
        CreateDocumentRequest createReq = new CreateDocumentRequest("author", "docName", "initiator");
        var created = documentService.create(createReq);
        transitionExecutor.submitOne(created.id(), "initiator", null);

        doThrow(new RuntimeException("DB error")).when(registryRepository).save(any(ApprovalRegistry.class));

        BatchTransitionRequest batchReq = new BatchTransitionRequest(List.of(created.id()), "approver", "comment");
        List<TransitionResult> results = transitionService.approveBatch(batchReq);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().result()).isEqualTo(ResultStatus.REGISTRY_ERROR);
        assertThat(results.getFirst().documentId()).isEqualTo(created.id());

        Document doc = documentRepository.findByIdWithAudits(created.id()).orElseThrow();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
        assertThat(doc.getAudits()).hasSize(2); // 1 CREATE + 1 SUBMIT
    }
}