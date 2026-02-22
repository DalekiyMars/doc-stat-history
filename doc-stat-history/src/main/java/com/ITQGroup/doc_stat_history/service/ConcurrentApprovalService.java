package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.dto.ConcurrentApprovalResult;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.entity.Document;
import com.ITQGroup.doc_stat_history.exception.DocumentNotFoundException;
import com.ITQGroup.doc_stat_history.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentApprovalService {

    private final TransitionExecutor transitionExecutor;
    private final DocumentRepository documentRepository;

    public ConcurrentApprovalResult testConcurrentApproval(
            Long documentId, int threads, int attempts, String initiator) {

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // attempts задач, каждая пытается утвердить один и тот же документ
        List<Callable<TransitionResult>> tasks = new ArrayList<>(attempts);
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> transitionExecutor.approveOne(documentId, initiator, "concurrent test"));
        }

        log.info("Concurrent approval START: documentId={}, threads={}, attempts={}",
                documentId, threads, attempts);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<TransitionResult>> futures = executor.invokeAll(tasks);

            for (Future<TransitionResult> future : futures) {
                TransitionResult result = future.get();
                switch (result.result()) {
                    case SUCCESS        -> successCount.incrementAndGet();
                    case REGISTRY_ERROR -> errorCount.incrementAndGet();
                    default             -> conflictCount.incrementAndGet();
                }
            }
        } catch (Exception e) {
            log.error("Concurrent approval test error: documentId={}", documentId, e);
        }

        Document finalDoc = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        log.info("Concurrent approval DONE: documentId={}, threads={}, attempts={}, " +
                        "success={}, conflict={}, error={}, finalStatus={}",
                documentId, threads, attempts,
                successCount.get(), conflictCount.get(), errorCount.get(), finalDoc.getStatus());

        if (successCount.get() != 1) {
            log.warn("Unexpected successCount={} for documentId={} — " +
                    "expected exactly 1 successful approval", successCount.get(), documentId);
        }

        return new ConcurrentApprovalResult(
                successCount.get(), conflictCount.get(), errorCount.get(), finalDoc.getStatus());
    }
}