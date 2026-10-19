ALTER TABLE products
    ADD COLUMN returnable BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE return_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    return_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    admin_note VARCHAR(500),
    qc_note VARCHAR(500),
    requested_quantity INT NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    upi_id VARCHAR(100),
    pickup_failures INT NOT NULL DEFAULT 0,
    completed_return BOOLEAN NOT NULL DEFAULT FALSE,
    replacement_order_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_return_public_id (public_id),
    KEY idx_return_order (order_id),
    KEY idx_return_item_status (order_item_id, status),
    CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_return_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_return_replacement_order FOREIGN KEY (replacement_order_id) REFERENCES orders(id)
) ENGINE=InnoDB;

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_request_id BIGINT NOT NULL,
    payment_id BIGINT NULL,
    gateway_refund_id VARCHAR(100) NULL,
    amount DECIMAL(12,2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_refund_return_request (return_request_id),
    UNIQUE KEY uk_refund_gateway_id (gateway_refund_id),
    UNIQUE KEY uk_refund_idempotency (idempotency_key),
    CONSTRAINT fk_refund_return FOREIGN KEY (return_request_id) REFERENCES return_requests(id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB;

ALTER TABLE shipments
    DROP INDEX uk_shipments_order,
    ADD COLUMN direction VARCHAR(20) NOT NULL DEFAULT 'FORWARD',
    ADD COLUMN return_request_id BIGINT NULL,
    ADD KEY idx_shipments_return_request (return_request_id),
    ADD KEY idx_shipments_order_direction (order_id, direction),
    ADD CONSTRAINT fk_shipments_return_request FOREIGN KEY (return_request_id) REFERENCES return_requests(id);

ALTER TABLE orders
    ADD COLUMN order_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN original_order_id BIGINT NULL,
    ADD CONSTRAINT fk_orders_original_order FOREIGN KEY (original_order_id) REFERENCES orders(id);
