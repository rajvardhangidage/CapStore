package com.capstore;

import com.capstore.entity.ShipmentStatus;
import com.capstore.exception.ApiException;
import com.capstore.service.MockCourierClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockCourierClientTest {
    private final MockCourierClient courier = new MockCourierClient();

    @Test
    void mapsCourierStatusesToTrackingStates() {
        assertEquals(ShipmentStatus.IN_TRANSIT, courier.mapStatus("in transit"));
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY, courier.mapStatus("out-for-delivery"));
        assertEquals(ShipmentStatus.SHIPPED, courier.mapStatus("picked_up"));
    }

    @Test
    void rejectsUnknownCourierStatus() {
        assertThrows(ApiException.class, () -> courier.mapStatus("lost"));
    }
}
