package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWorkers {

    private final DocumentRepository documentRepository;
    private final TransitionService transitionService;

    @Value("${app.worker.batch-size:50}")
    private int batchSize;

    // SUBMIT-worker: каждые 30 секунд забирает пачку DRAFT-документов и отправляет на согласование.
    @Scheduled(fixedDelayString = "${app.worker.submit-delay-ms:30000}")
    public void submitWorker() {
        List<Long> ids = documentRepository.findIdsByStatus(
                DocumentStatus.DRAFT.name(), batchSize);
        if (ids.isEmpty()) return;

        log.info("SUBMIT-worker: found {} DRAFT documents", ids.size());
        var request = new BatchTransitionRequest(ids, "system-submit-worker", null);
        var results = transitionService.submitBatch(request);
        long success = results.stream().filter(r ->
                Objects.equals(r.result(), ResultStatus.SUCCESS)).count();
        log.info("SUBMIT-worker: processed={}, success={}", ids.size(), success);
    }

    // APPROVE-worker: каждые 30 секунд забирает пачку SUBMITTED-документов и утверждает.
    @Scheduled(fixedDelayString = "${app.worker.approve-delay-ms:30000}")
    public void approveWorker() {
        List<Long> ids = documentRepository.findIdsByStatus(
                DocumentStatus.SUBMITTED.name(), batchSize);
        if (ids.isEmpty()) return;

        log.info("APPROVE-worker: found {} SUBMITTED documents", ids.size());
        var request = new BatchTransitionRequest(ids, "system-approve-worker", null);
        var results = transitionService.approveBatch(request);
        long success = results.stream().filter(r ->
                Objects.equals(r.result(), ResultStatus.SUCCESS)).count();
        log.info("APPROVE-worker: processed={}, success={}", ids.size(), success);
    }
}