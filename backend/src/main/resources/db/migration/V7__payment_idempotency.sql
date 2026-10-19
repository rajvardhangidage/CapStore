ALTER TABLE orders
    ADD COLUMN checkout_request_key VARCHAR(100) NULL,
    ADD UNIQUE KEY uk_orders_user_checkout_request (user_id, checkout_request_key);
