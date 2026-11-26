ALTER TABLE orders
    ADD INDEX idx_orders_user_created (user_id, created_at),
    ADD INDEX idx_orders_status_created (status, created_at),
    ADD INDEX idx_orders_payment_created (payment_status, created_at),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE products
    ADD INDEX idx_products_active_category_created (active, category_id, created_at),
    ADD INDEX idx_products_active_price (active, base_price),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE product_variants
    ADD INDEX idx_variants_stock_product (stock_qty, product_id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE shipments
    ADD INDEX idx_shipments_order_direction_return (order_id, direction, return_request_id, id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE return_requests
    ADD INDEX idx_return_requests_status_pickup (status, pickup_failures),
    ADD INDEX idx_return_requests_order_created (order_id, created_at),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE refunds
    ADD INDEX idx_refunds_status_attempts_created (status, attempts, created_at),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE addresses
    ADD INDEX idx_addresses_user_default_id (user_id, default_address, id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE banners
    ADD INDEX idx_banners_active_sort_id (active, sort_order, id),
    ALGORITHM=INPLACE,
    LOCK=NONE;
