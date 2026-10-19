CREATE TABLE shipments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  courier_name VARCHAR(100) NOT NULL,
  awb VARCHAR(100) NOT NULL,
  tracking_url VARCHAR(500),
  expected_delivery DATE,
  status VARCHAR(30) NOT NULL,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_shipments_order (order_id),
  UNIQUE KEY uk_shipments_courier_awb (courier_name, awb),
  CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;

CREATE TABLE tracking_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  shipment_id BIGINT NOT NULL,
  courier_name VARCHAR(100) NOT NULL,
  event_id VARCHAR(190) NOT NULL,
  status VARCHAR(30) NOT NULL,
  description VARCHAR(500),
  occurred_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_tracking_courier_event (courier_name, event_id),
  KEY idx_tracking_shipment_time (shipment_id, occurred_at),
  CONSTRAINT fk_tracking_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id)
) ENGINE=InnoDB;

CREATE TABLE notification_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  notification_type VARCHAR(40) NOT NULL,
  awb VARCHAR(100) NOT NULL DEFAULT '',
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_notification_order_type_awb (order_id, notification_type, awb),
  CONSTRAINT fk_notification_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;
