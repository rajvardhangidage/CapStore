package com.capstore.service;

import com.capstore.entity.ShipmentStatus;
import com.capstore.entity.CustomerOrder;
import com.capstore.entity.ReturnRequest;

public interface CourierClient {
    ShipmentStatus mapStatus(String courierStatus);
    ReverseShipment createReverseShipment(CustomerOrder order, ReturnRequest request);
    ReverseShipment createReturnToCustomer(CustomerOrder order, ReturnRequest request);

    record ReverseShipment(String courierName, String awb, String trackingUrl) {}
}
