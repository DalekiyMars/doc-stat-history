package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.DocStatHistoryApplication;
import com.ITQGroup.doc_stat_history.TestcontainersConfiguration;
import com.ITQGroup.doc_stat_history.common.ActionType;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import com.ITQGroup.doc_stat_history.util.JsonUtils;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DocStatHistoryApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers
class TransitionServiceTest {

    private static final String BASE_PATH = "src/test/resources/test-data/";

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private TransitionExecutor transitionExecutor;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    void testSubmitBatch_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest createReq = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        var created = documentService.create(createReq);

        BatchTransitionRequest req = new BatchTransitionRequest(List.of(created.id()), "test-initiator", "comment");
        List<TransitionResult> results = transitionService.submitBatch(req);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().result()).isEqualTo(ResultStatus.SUCCESS);

        Document doc = documentRepository.findByIdWithAudits(created.id()).orElseThrow();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
        assertThat(doc.getAudits()).hasSize(2); // CREATE + SUBMIT логи
        assertThat(doc.getAudits().getLast().getActionType()).isEqualTo(ActionType.SUBMIT);
    }

    @Test
    void testApproveBatch_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest createReq = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        var created = documentService.create(createReq);

        transitionExecutor.submitOne(created.id(), "init", null);

        BatchTransitionRequest req = new BatchTransitionRequest(List.of(created.id()), "test-approver", "comment");
        List<TransitionResult> results = transitionService.approveBatch(req);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().result()).isEqualTo(ResultStatus.SUCCESS);

        Document doc = documentRepository.findByIdWithAudits(created.id()).orElseThrow();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(doc.getAudits()).hasSize(3);// CREATE + SUBMIT + APPROVE
        assertThat(doc.getAudits().getLast().getActionType()).isEqualTo(ActionType.APPROVE);
    }

    @Test
    void testSubmitBatch_Conflict() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest createReq = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        var created = documentService.create(createReq);

        transitionExecutor.submitOne(created.id(), "init", null);

        BatchTransitionRequest req = new BatchTransitionRequest(List.of(created.id()), "test-initiator", null);
        List<TransitionResult> results = transitionService.submitBatch(req);

        assertThat(results.getFirst().result()).isEqualTo(ResultStatus.CONFLICT);

        Document doc = documentRepository.findById(created.id()).orElseThrow();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
    }
}