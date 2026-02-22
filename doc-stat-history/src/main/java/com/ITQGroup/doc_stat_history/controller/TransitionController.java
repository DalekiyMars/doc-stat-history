package com.ITQGroup.doc_stat_history.controller;

import com.ITQGroup.doc_stat_history.dto.BatchTransitionRequest;
import com.ITQGroup.doc_stat_history.dto.TransitionResult;
import com.ITQGroup.doc_stat_history.service.TransitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class TransitionController {

    private final TransitionService transitionService;

    // Пакетный перевод DRAFT -> SUBMITTED.
    // Возвращает результат по каждому id: SUCCESS / CONFLICT / NOT_FOUND.
    @PostMapping("/submit")
    public List<TransitionResult> submit(@Valid @RequestBody BatchTransitionRequest request) {
        return transitionService.submitBatch(request);
    }

    // Пакетный перевод SUBMITTED -> APPROVED.
    // Дополнительный статус результата: REGISTRY_ERROR — документ остался в SUBMITTED.
    @PostMapping("/approve")
    public List<TransitionResult> approve(@Valid @RequestBody BatchTransitionRequest request) {
        return transitionService.approveBatch(request);
    }
}