package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.DocStatHistoryApplication;
import com.ITQGroup.doc_stat_history.TestcontainersConfiguration;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.ConcurrentApprovalResult;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DocStatHistoryApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers
class ConcurrentApprovalServiceTest {

    @Autowired
    private ConcurrentApprovalService concurrentApprovalService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private TransitionExecutor transitionExecutor;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    void testConcurrentApproval_onlyOneSucceeds() {
        // given
        CreateDocumentRequest createReq = new CreateDocumentRequest("author", "doc", "init");
        var created = documentService.create(createReq);
        transitionExecutor.submitOne(created.id(), "init", null); // теперь SUBMITTED

        ConcurrentApprovalResult result = concurrentApprovalService.testConcurrentApproval(
                created.id(), 5, 10, "concurrent-tester");

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.conflictCount() + result.errorCount()).isEqualTo(9); // остальные попытки не успешны
        assertThat(result.finalStatus()).isEqualTo(DocumentStatus.APPROVED);
    }
}