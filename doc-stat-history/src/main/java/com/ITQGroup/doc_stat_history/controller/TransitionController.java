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

    // POST /api/v1/documents/submit
    // Пакетный перевод DRAFT -> SUBMITTED.
    // Возвращает результат по каждому id: SUCCESS / CONFLICT / NOT_FOUND.
    // HTTP 200 всегда — частичные ошибки не являются ошибкой всего запроса.
    @PostMapping("/submit")
    public List<TransitionResult> submit(@Valid @RequestBody BatchTransitionRequest request) {
        return transitionService.submitBatch(request);
    }

    // POST /api/v1/documents/approve
    // Пакетный перевод SUBMITTED -> APPROVED.
    // Дополнительный статус результата: REGISTRY_ERROR — документ остался в SUBMITTED.
    // HTTP 200 всегда — атомарность на уровне документа, не запроса.
    @PostMapping("/approve")
    public List<TransitionResult> approve(@Valid @RequestBody BatchTransitionRequest request) {
        return transitionService.approveBatch(request);
    }
}