package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.DocStatHistoryApplication;
import com.ITQGroup.doc_stat_history.TestcontainersConfiguration;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.BatchCreateRequest;
import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.repository.ApprovalRegistryRepository;
import com.ITQGroup.doc_stat_history.repository.AuditRepository;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import com.ITQGroup.doc_stat_history.util.JsonUtils;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = DocStatHistoryApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers
class DocumentWorkersTest {

    private static final String BASE_PATH = "src/test/resources/test-data/";

    @Autowired
    private DocumentWorkers documentWorkers;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ApprovalRegistryRepository approvalRegistryRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private TransitionService transitionService;

    @BeforeEach
    @Transactional
    void setUp() {
        approvalRegistryRepository.deleteAll();
        auditRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    void testSubmitWorker_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "batch-create-request.json";
        List<CreateDocumentRequest> reqs = JsonUtils.convertJsonFromFileToList(jsonPath, CreateDocumentRequest.class);
        BatchCreateRequest batchRequest = new BatchCreateRequest(reqs);
        documentService.createBatch(batchRequest);

        documentWorkers.submitWorker();

        await().atMost(10, SECONDS)
                .untilAsserted(() ->
                        assertThat(documentRepository.countByStatus(DocumentStatus.SUBMITTED)).isEqualTo(2)
                );
        assertThat(auditRepository.count()).isEqualTo(4); //2 лога на создание и 2 на перевод статуса
    }

    @Test
    void testApproveWorker_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "batch-create-request.json";
        List<CreateDocumentRequest> reqs = JsonUtils.convertJsonFromFileToList(jsonPath, CreateDocumentRequest.class);
        BatchCreateRequest batchRequest = new BatchCreateRequest(reqs);
        var created = documentService.createBatch(batchRequest);

        List<Long> ids = created.stream().map(DocumentResponse::id).toList();
        transitionService.submitBatch(new BatchTransitionRequest(ids, "init", null));

        documentWorkers.approveWorker();

        await().atMost(10, SECONDS)
                .untilAsserted(() ->
                        assertThat(documentRepository.countByStatus(DocumentStatus.APPROVED)).isEqualTo(2)
                );

        assertThat(approvalRegistryRepository.count()).isEqualTo(2);
        assertThat(auditRepository.count()).isEqualTo(6); //2 логи + 2 submit + 2 approve
    }
}