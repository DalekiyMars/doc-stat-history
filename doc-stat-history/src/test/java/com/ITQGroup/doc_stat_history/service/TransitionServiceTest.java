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

    @Test
    void testApproveBatch_PartialResults() throws IOException {
        // создаём три документа DRAFT
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest createReq = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        var doc1 = documentService.create(createReq);
        var doc2 = documentService.create(createReq);
        var doc3 = documentService.create(createReq);

        // переводим doc1 и doc2 в SUBMITTED
        transitionExecutor.submitOne(doc1.id(), "init", null);
        transitionExecutor.submitOne(doc2.id(), "init", null);

        // теперь пытаемся утвердить doc1 (SUBMITTED), doc2 (SUBMITTED), doc3 (DRAFT) и несуществующий id
        List<Long> ids = List.of(doc1.id(), doc2.id(), doc3.id(), 999L);
        BatchTransitionRequest req = new BatchTransitionRequest(ids, "approver", null);

        List<TransitionResult> results = transitionService.approveBatch(req);

        assertThat(results).hasSize(4);
        // doc1 – успех
        assertThat(results.get(0).result()).isEqualTo(ResultStatus.SUCCESS);
        assertThat(results.get(0).documentId()).isEqualTo(doc1.id());
        // doc2 – успех
        assertThat(results.get(1).result()).isEqualTo(ResultStatus.SUCCESS);
        assertThat(results.get(1).documentId()).isEqualTo(doc2.id());
        // doc3 – конфликт
        assertThat(results.get(2).result()).isEqualTo(ResultStatus.CONFLICT);
        // 999 – не найдено
        assertThat(results.get(3).result()).isEqualTo(ResultStatus.NOT_FOUND);

        // проверяем статусы
        assertThat(documentRepository.findById(doc1.id()).orElseThrow().getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(documentRepository.findById(doc2.id()).orElseThrow().getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(documentRepository.findById(doc3.id()).orElseThrow().getStatus()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    void testSubmitBatch_PartialResults() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest createReq = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        var doc1 = documentService.create(createReq); // DRAFT
        var doc2 = documentService.create(createReq); // DRAFT
        // переводим doc2 в SUBMITTED
        transitionExecutor.submitOne(doc2.id(), "init", null);

        List<Long> ids = List.of(doc1.id(), doc2.id(), 999L);
        BatchTransitionRequest req = new BatchTransitionRequest(ids, "submitter", null);

        List<TransitionResult> results = transitionService.submitBatch(req);

        assertThat(results).hasSize(3);
        // doc1 – успех
        assertThat(results.get(0).result()).isEqualTo(ResultStatus.SUCCESS);
        // doc2 – конфликт
        assertThat(results.get(1).result()).isEqualTo(ResultStatus.CONFLICT);
        // 999 – не найдено
        assertThat(results.get(2).result()).isEqualTo(ResultStatus.NOT_FOUND);

        // проверяем статусы
        assertThat(documentRepository.findById(doc1.id()).orElseThrow().getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
        assertThat(documentRepository.findById(doc2.id()).orElseThrow().getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
    }
}