ALTER TABLE products ADD COLUMN public_id CHAR(36) NULL;
UPDATE products SET public_id = UUID() WHERE public_id IS NULL;
ALTER TABLE products MODIFY public_id CHAR(36) NOT NULL;
ALTER TABLE products ADD UNIQUE KEY uk_products_public_id (public_id);

ALTER TABLE orders ADD COLUMN public_id CHAR(36) NULL;
UPDATE orders SET public_id = UUID() WHERE public_id IS NULL;
ALTER TABLE orders MODIFY public_id CHAR(36) NOT NULL;
ALTER TABLE orders ADD UNIQUE KEY uk_orders_public_id (public_id);
