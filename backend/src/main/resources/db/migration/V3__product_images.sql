CREATE TABLE product_images (
  product_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  PRIMARY KEY (product_id, sort_order),
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO product_images (product_id, sort_order, image_url)
SELECT id, 0, image_url FROM products WHERE image_url IS NOT NULL AND image_url <> '';
