package com.ITQGroup.doc_stat_history.controller;

import com.ITQGroup.doc_stat_history.dto.ConcurrentApprovalResult;
import com.ITQGroup.doc_stat_history.service.ConcurrentApprovalService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class ConcurrentApprovalController {

    private final ConcurrentApprovalService concurrentApprovalService;

    // Запускает attempts параллельных попыток утвердить документ.
    @PostMapping("/{id}/concurrent-approve")
    public ConcurrentApprovalResult concurrentApprove(
            @PathVariable Long id,
            @RequestParam @Min(1) @Max(50) int threads,
            @RequestParam @Min(1) @Max(100) int attempts,
            @RequestParam @NotBlank String initiator) {
        return concurrentApprovalService.testConcurrentApproval(id, threads, attempts, initiator);
    }
}