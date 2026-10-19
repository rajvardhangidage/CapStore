package com.capstore.service;

import com.capstore.dto.ExtraDtos.StoreConfig;
import com.capstore.dto.ExtraDtos.StoreSettingsRequest;
import com.capstore.entity.StoreSettings;
import com.capstore.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {
    private static final long SETTINGS_ID = 1L;

    private final StoreSettingsRepository settings;

    @Value("${app.store.name}") private String storeName;

    @Transactional(readOnly = true)
    public StoreConfig get() {
        StoreSettings current = settings.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Store settings have not been initialized"));
        return new StoreConfig(storeName, current.getGstRatePercent(), current.getShippingFlatRate(),
                current.getFreeShippingAbove());
    }

    @Transactional
    public StoreConfig update(StoreSettingsRequest request) {
        StoreSettings current = settings.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Store settings have not been initialized"));
        current.setGstRatePercent(request.gstRatePercent());
        current.setShippingFlatRate(request.shippingFlatRate());
        current.setFreeShippingAbove(request.freeShippingAbove());
        return get();
    }
}
