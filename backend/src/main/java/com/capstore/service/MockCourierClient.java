package com.capstore.service;

import com.capstore.entity.ShipmentStatus;
import com.capstore.entity.CustomerOrder;
import com.capstore.entity.ReturnRequest;
import com.capstore.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class MockCourierClient implements CourierClient {
    private static final Map<String, ShipmentStatus> STATUS_MAP = Map.ofEntries(
            Map.entry("CREATED", ShipmentStatus.CREATED),
            Map.entry("SHIPMENT_CREATED", ShipmentStatus.CREATED),
            Map.entry("PICKED_UP", ShipmentStatus.SHIPPED),
            Map.entry("SHIPPED", ShipmentStatus.SHIPPED),
            Map.entry("IN_TRANSIT", ShipmentStatus.IN_TRANSIT),
            Map.entry("OUT_FOR_DELIVERY", ShipmentStatus.OUT_FOR_DELIVERY),
            Map.entry("DELIVERED", ShipmentStatus.DELIVERED));

    @Override
    public ShipmentStatus mapStatus(String courierStatus) {
        String normalized = courierStatus.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        ShipmentStatus status = STATUS_MAP.get(normalized);
        if (status == null) throw ApiException.badRequest("Unsupported courier status");
        return status;
    }

    @Override
    public ReverseShipment createReverseShipment(CustomerOrder order, ReturnRequest request) {
        String awb = "RET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return new ReverseShipment("MOCK-COURIER", awb,
                "https://tracking.example/returns/" + request.getPublicId());
    }

    @Override
    public ReverseShipment createReturnToCustomer(CustomerOrder order, ReturnRequest request) {
        String awb = "RTS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return new ReverseShipment("MOCK-COURIER", awb,
                "https://tracking.example/returns-to-customer/" + request.getPublicId());
    }
}
