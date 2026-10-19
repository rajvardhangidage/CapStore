package com.capstore;

import com.capstore.repository.ShipmentRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipmentLockingTest {
    @Test
    void shipmentMutationsUsePessimisticWriteLocks() throws Exception {
        Lock shipmentByOrder = ShipmentRepository.class
                .getMethod("findByOrderIdForUpdate", Long.class).getAnnotation(Lock.class);
        Lock shipmentByAwb = ShipmentRepository.class
                .getMethod("findByCourierNameAndAwbForUpdate", String.class, String.class).getAnnotation(Lock.class);

        assertEquals(LockModeType.PESSIMISTIC_WRITE, shipmentByOrder.value());
        assertEquals(LockModeType.PESSIMISTIC_WRITE, shipmentByAwb.value());
    }
}
