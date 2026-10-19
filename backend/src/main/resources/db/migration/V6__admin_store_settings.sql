CREATE TABLE store_settings (
    id BIGINT NOT NULL PRIMARY KEY,
    gst_rate_percent DECIMAL(5,2) NOT NULL,
    shipping_flat_rate DECIMAL(10,2) NOT NULL,
    free_shipping_above DECIMAL(10,2) NOT NULL
);

INSERT INTO store_settings (id, gst_rate_percent, shipping_flat_rate, free_shipping_above)
VALUES (1, 5.00, 70.00, 999.00);

ALTER TABLE orders
    ADD COLUMN gst_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 5.00;
