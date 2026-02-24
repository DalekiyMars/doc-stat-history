package com.ITQGroup.doc_stat_history.service;

import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Делегирует каждый документ в TransitionExecutor.
 * Транзакции управляются на уровне TransitionExecutor.
 */
@Service
@RequiredArgsConstructor
public class TransitionService {
    private final TransitionExecutor transitionExecutor;

    public List<TransitionResult> submitBatch(BatchTransitionRequest request) {
        return request.ids().stream()
                .map(id -> transitionExecutor.submitOne(id, request.initiator(), request.comment()))
                .toList();
    }

    public List<TransitionResult> approveBatch(BatchTransitionRequest request) {
        return request.ids().stream()
                .map(id -> transitionExecutor.approveOne(id, request.initiator(), request.comment()))
                .toList();
    }
}