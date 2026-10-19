package com.capstore.dto;

import com.capstore.entity.ReturnStatus;
import com.capstore.entity.ReturnType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReturnDtos {
    public record CreateReturnRequest(@NotBlank @Size(max = 36) String orderId,
                                     @NotNull Long orderItemId, @NotNull ReturnType type,
                                     @NotBlank @Size(max = 1000) String reason,
                                     @Min(1) int quantity,
                                     @Size(max = 100) @Pattern(regexp = "^[A-Za-z0-9._-]+@[A-Za-z0-9.-]+$") String upiId) {}
    public record ReturnDecisionRequest(@NotBlank @Size(max = 500) String note) {}
    public record QualityCheckRequest(boolean passed, @Size(max = 500) String note) {}
    public record ReturnResponse(String id, String orderId, String replacementOrderId, Long orderItemId,
                                  String productName, ReturnType type, ReturnStatus status, String reason,
                                  String adminNote, String qcNote, int quantity, BigDecimal refundAmount,
                                  String upiId, String courier, String awb, String trackingUrl,
                                  int pickupFailures, LocalDateTime createdAt, boolean manualRefund) {}
}
