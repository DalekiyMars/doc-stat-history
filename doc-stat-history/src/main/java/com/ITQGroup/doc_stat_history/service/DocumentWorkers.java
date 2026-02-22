package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.DocumentStatus;
import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWorkers {

    private final DocumentRepository documentRepository;
    private final TransitionService transitionService;

    @Value("${app.worker.batch-size:50}")
    private int batchSize;

    // Флаги защищают от параллельного запуска одного и того же воркера.
    private final AtomicBoolean submitRunning  = new AtomicBoolean(false);
    private final AtomicBoolean approveRunning = new AtomicBoolean(false);


    // Тело уходит выполняться в workerExecutor. Оба воркера работают параллельно.
    @Async("workerExecutor")
    @Scheduled(fixedDelayString = "${app.worker.submit-delay-ms:30000}")
    public void submitWorker() {
        // Если уже запущен — пропускаем тик
        if (!submitRunning.compareAndSet(false, true)) {
            log.warn("SUBMIT-worker: previous run still active, skipping tick");
            return;
        }
        try {
            long totalRemaining = documentRepository.countByStatus(DocumentStatus.DRAFT);
            if (totalRemaining == 0) return;

            List<Long> ids = documentRepository.findIdsByStatus(
                    DocumentStatus.DRAFT.name(), batchSize);
            log.info("SUBMIT-worker START: batch={}, totalDraft={}",
                    ids.size(), totalRemaining);

            long startMs = System.currentTimeMillis();
            var request = new BatchTransitionRequest(ids, "system-submit-worker", null);
            List<TransitionResult> results = transitionService.submitBatch(request);
            long elapsedMs = System.currentTimeMillis() - startMs;

            logResults("SUBMIT-worker", results, elapsedMs);

            long stillRemaining = documentRepository.countByStatus(DocumentStatus.DRAFT);
            log.info("SUBMIT-worker DONE: elapsed={}ms, remaining DRAFT={}",
                    elapsedMs, stillRemaining);
        } finally {
            submitRunning.set(false);
        }
    }

    @Async("workerExecutor")
    @Scheduled(fixedDelayString = "${app.worker.approve-delay-ms:30000}")
    public void approveWorker() {
        if (!approveRunning.compareAndSet(false, true)) {
            log.warn("APPROVE-worker: previous run still active, skipping tick");
            return;
        }
        try {
            long totalRemaining = documentRepository.countByStatus(DocumentStatus.SUBMITTED);
            if (totalRemaining == 0) return;

            List<Long> ids = documentRepository.findIdsByStatus(
                    DocumentStatus.SUBMITTED.name(), batchSize);
            log.info("APPROVE-worker START: batch={}, totalSubmitted={}",
                    ids.size(), totalRemaining);

            long startMs = System.currentTimeMillis();
            var request = new BatchTransitionRequest(ids, "system-approve-worker", null);
            List<TransitionResult> results = transitionService.approveBatch(request);
            long elapsedMs = System.currentTimeMillis() - startMs;

            logResults("APPROVE-worker", results, elapsedMs);

            long stillRemaining = documentRepository.countByStatus(DocumentStatus.SUBMITTED);
            log.info("APPROVE-worker DONE: elapsed={}ms, remaining SUBMITTED={}",
                    elapsedMs, stillRemaining);
        } finally {
            approveRunning.set(false);
        }
    }

    private void logResults(String workerName, List<TransitionResult> results, long elapsedMs) {
        Map<ResultStatus, Long> summary = results.stream()
                .collect(Collectors.groupingBy(TransitionResult::result, Collectors.counting()));

        log.info("{}: processed={}, summary={}, elapsed={}ms",
                workerName, results.size(), summary, elapsedMs);

        results.stream()
                .filter(r -> r.result() != ResultStatus.SUCCESS)
                .forEach(r -> log.warn("{}: id={} -> {} | {}",
                        workerName, r.documentId(), r.result(), r.message()));
    }
}