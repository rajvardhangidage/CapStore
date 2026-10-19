package com.capstore.service;

import com.capstore.entity.InventoryLog;
import com.capstore.entity.ProductVariant;
import com.capstore.exception.ApiException;
import com.capstore.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryLogRepository logs;

    /** Caller must have loaded the variant with a write lock (findByIdForUpdate). */
    public void adjust(ProductVariant v, int delta, String reason, Long refId) {
        int updated = v.getStockQty() + delta;
        if (updated < 0) {
            throw ApiException.badRequest("Insufficient stock for " + v.getSku());
        }
        v.setStockQty(updated);
        InventoryLog log = new InventoryLog();
        log.setVariantId(v.getId());
        log.setChangeQty(delta);
        log.setReason(reason);
        log.setRefId(refId);
        logs.save(log);
    }
}
