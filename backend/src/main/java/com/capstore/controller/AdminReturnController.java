package com.capstore.controller;

import com.capstore.dto.ReturnDtos.*;
import com.capstore.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/returns")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReturnController {
    private final ReturnService returns;

    @GetMapping
    public List<ReturnResponse> list() { return returns.allForAdmin(); }

    @PostMapping("/{id}/approve")
    public ReturnResponse approve(@PathVariable String id, @Valid @RequestBody ReturnDecisionRequest request) {
        return returns.approve(id, request.note());
    }

    @PostMapping("/{id}/reject")
    public ReturnResponse reject(@PathVariable String id, @Valid @RequestBody ReturnDecisionRequest request) {
        return returns.reject(id, request.note());
    }

    @PostMapping("/{id}/quality-check")
    public ReturnResponse qualityCheck(@PathVariable String id, @Valid @RequestBody QualityCheckRequest request) {
        return returns.qualityCheck(id, request.passed(), request.note());
    }

    @PostMapping("/{id}/refund-confirmed")
    public ReturnResponse codRefundConfirmed(@PathVariable String id,
                                             @Valid @RequestBody ReturnDecisionRequest request) {
        return returns.confirmCodRefund(id, request.note());
    }
}
