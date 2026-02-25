package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.DocStatHistoryApplication;
import com.ITQGroup.doc_stat_history.TestcontainersConfiguration;
import com.ITQGroup.doc_stat_history.common.ActionType;
import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.dto.BatchCreateRequest;
import com.ITQGroup.doc_stat_history.dto.CreateDocumentRequest;
import com.ITQGroup.doc_stat_history.dto.CursorResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentResponse;
import com.ITQGroup.doc_stat_history.dto.DocumentSearchRequest;
import com.ITQGroup.doc_stat_history.exception.DocumentNotFoundException;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import com.ITQGroup.doc_stat_history.util.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(classes = DocStatHistoryApplication.class)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    private final String BASE_PATH = "src/test/resources/test-data/";

    @Test
    @Transactional
    void testCreateSingle_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest request = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);

        DocumentResponse response = documentService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isPositive();
        assertThat(response.uniqueNumber()).startsWith("DOC-");
        assertThat(response.author()).isEqualTo(request.author());
        assertThat(response.docName()).isEqualTo(request.docName());
        assertThat(response.status()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.lastUpdate()).isNotNull();

        DocumentResponse withHistory = documentService.getWithAudits(response.id());
        assertThat(withHistory.audits()).hasSize(1);
        assertThat(withHistory.audits().getFirst().actionType()).isEqualTo(ActionType.CREATE);
        assertThat(withHistory.audits().getFirst().actionAuthor()).isEqualTo(request.initiator());
    }

    @Test
    @Transactional
    void testCreateBatch_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "batch-create-request.json";
        List<CreateDocumentRequest> reqs = JsonUtils.convertJsonFromFileToList(jsonPath, CreateDocumentRequest.class);
        BatchCreateRequest batchRequest = new BatchCreateRequest(reqs);

        List<DocumentResponse> responses = documentService.createBatch(batchRequest);

        assertThat(responses).hasSize(2);
        responses.forEach(resp -> {
            assertThat(resp.status()).isEqualTo(DocumentStatus.DRAFT);
            assertThat(resp.uniqueNumber()).startsWith("DOC-");
        });

        assertThat(documentRepository.count()).isEqualTo(2);
    }

    @Test
    void testGetById_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest request = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        DocumentResponse created = documentService.create(request);

        DocumentResponse got = documentService.getById(created.id());

        assertThat(got.author()).isEqualTo(created.author());
        assertThat(got.uniqueNumber()).isEqualTo(created.uniqueNumber());
        assertThat(got.status()).isEqualTo(created.status());
        assertThat(got.id()).isEqualTo(created.id());
        assertThat(got.docName()).isEqualTo(created.docName());
    }

    @Test
    void testSearch_HappyPath() throws IOException {
        String createPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest req = JsonUtils.convertJsonFromFileToObject(createPath, CreateDocumentRequest.class);
        documentService.create(req);  // DRAFT

        LocalDateTime now = LocalDateTime.now();
        DocumentSearchRequest searchReq = new DocumentSearchRequest(
                DocumentStatus.DRAFT,
                "test-author",
                now.minusDays(1),
                now.plusDays(1)
        );

        CursorResponse<DocumentResponse> response = documentService.search(searchReq, 10, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().status()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    void testGetById_NotFound() {
        assertThatThrownBy(() -> documentService.getById(999L))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found: 999");
    }

    @Test
    void testGetWithAudits_HappyPath_NoAudits() throws IOException {
        String jsonPath = BASE_PATH + "create-request.json";
        CreateDocumentRequest request = JsonUtils.convertJsonFromFileToObject(jsonPath, CreateDocumentRequest.class);
        DocumentResponse created = documentService.create(request);

        DocumentResponse got = documentService.getWithAudits(created.id());

        assertThat(got.id()).isEqualTo(created.id());
        assertThat(got.audits()).hasSize(1);
        assertThat(got.audits().getFirst().actionType()).isEqualTo(ActionType.CREATE);
    }

    @Test
    void testGetByIds_HappyPath() throws IOException {
        String jsonPath = BASE_PATH + "batch-create-request.json";
        List<CreateDocumentRequest> list = JsonUtils.convertJsonFromFileToList(jsonPath, CreateDocumentRequest.class);
        BatchCreateRequest request = new BatchCreateRequest(list);

        List<DocumentResponse> created = documentService.createBatch(request);

        List<Long> ids = created.stream().map(DocumentResponse::id).toList();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        var slice = documentService.getByIds(ids, pageable);

        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.getContent().getFirst().id()).isEqualTo(created.get(1).id());
    }

    @Test
    void testSearch_WithNoResults() {
        LocalDateTime now = LocalDateTime.now();
        DocumentSearchRequest searchReq = new DocumentSearchRequest(
                DocumentStatus.DRAFT,
                "non-existent-author",
                now.minusDays(1),
                now.plusDays(1)
        );

        CursorResponse<DocumentResponse> response = documentService.search(searchReq, 10, null);

        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void testSearch_WithCursor() throws IOException {
        String jsonPath = BASE_PATH + "batch-create-request.json";
        List<CreateDocumentRequest> list = JsonUtils.convertJsonFromFileToList(jsonPath, CreateDocumentRequest.class);
        BatchCreateRequest request = new BatchCreateRequest(list);
        documentService.createBatch(request);  // 2 docs

        DocumentSearchRequest searchReq = new DocumentSearchRequest(null, null, null, null);

        CursorResponse<DocumentResponse> first = documentService.search(searchReq, 1, null);
        assertThat(first.content()).hasSize(1);
        Long cursor = first.nextCursor();
        assertThat(cursor).isNotNull();

        CursorResponse<DocumentResponse> second = documentService.search(searchReq, 1, cursor);
        assertThat(second.content()).hasSize(1);
        assertThat(second.content().getFirst().id()).isGreaterThan(first.content().getFirst().id());
    }
}