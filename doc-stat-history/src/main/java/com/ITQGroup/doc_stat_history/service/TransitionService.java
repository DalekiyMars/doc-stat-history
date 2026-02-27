package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.common.ResultStatus;
import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Делегирует каждый документ в TransitionExecutor.
 * Транзакции управляются на уровне TransitionExecutor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransitionService {
    private final TransitionExecutor transitionExecutor;

    public List<TransitionResult> submitBatch(BatchTransitionRequest request) {
        return request.ids().stream()
                .map(id -> {
                    try {
                        return transitionExecutor.submitOne(id, request.initiator(), request.comment());
                    } catch (Exception e) {
                        log.error("Submit failed for id {}: {}", id, e.getMessage(), e);
                        return new TransitionResult(id, ResultStatus.ERROR, e.getMessage());
                    }
                })
                .toList();
    }

    public List<TransitionResult> approveBatch(BatchTransitionRequest request) {
        return request.ids().stream()
                .map(id -> {
                    try {
                        return transitionExecutor.approveOne(id, request.initiator(), request.comment());
                    } catch (Exception e) {
                        log.error("Approve failed for id {}: {}", id, e.getMessage());
                        return new TransitionResult(id, ResultStatus.REGISTRY_ERROR, e.getMessage());
                    }
                })
                .toList();
    }
}