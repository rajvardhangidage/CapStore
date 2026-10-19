package com.capstore.service;

import com.capstore.entity.ReturnRequest;
import com.capstore.entity.ReturnStatus;
import com.capstore.exception.ApiException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

final class ReturnTransitions {
    private static final Map<ReturnStatus, EnumSet<ReturnStatus>> ALLOWED = new EnumMap<>(ReturnStatus.class);
    static {
        ALLOWED.put(ReturnStatus.REQUESTED, EnumSet.of(ReturnStatus.APPROVED, ReturnStatus.REJECTED));
        ALLOWED.put(ReturnStatus.APPROVED, EnumSet.of(ReturnStatus.PICKUP_SCHEDULED));
        ALLOWED.put(ReturnStatus.PICKUP_SCHEDULED, EnumSet.of(ReturnStatus.PICKED_UP, ReturnStatus.CLOSED));
        ALLOWED.put(ReturnStatus.PICKED_UP, EnumSet.of(ReturnStatus.RECEIVED));
        ALLOWED.put(ReturnStatus.RECEIVED, EnumSet.of(ReturnStatus.QC_PASSED, ReturnStatus.QC_FAILED));
        ALLOWED.put(ReturnStatus.QC_PASSED, EnumSet.of(ReturnStatus.REFUND_INITIATED,
                ReturnStatus.REPLACEMENT_CREATED));
        ALLOWED.put(ReturnStatus.REFUND_INITIATED, EnumSet.of(ReturnStatus.REFUNDED));
        ALLOWED.put(ReturnStatus.REFUNDED, EnumSet.of(ReturnStatus.CLOSED));
        ALLOWED.put(ReturnStatus.REPLACEMENT_CREATED, EnumSet.of(ReturnStatus.CLOSED));
        ALLOWED.put(ReturnStatus.QC_FAILED, EnumSet.of(ReturnStatus.CLOSED));
    }

    private ReturnTransitions() {}

    static void move(ReturnRequest request, ReturnStatus next) {
        if (!ALLOWED.getOrDefault(request.getStatus(), EnumSet.noneOf(ReturnStatus.class)).contains(next))
            throw ApiException.conflict("Invalid return status transition: " + request.getStatus() + " -> " + next);
        request.setStatus(next);
    }
}
