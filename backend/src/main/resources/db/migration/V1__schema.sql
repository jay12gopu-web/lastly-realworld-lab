CREATE TABLE products (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(150) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  stock_quantity INT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_products_price CHECK (price >= 0),
  CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE customer_orders (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  idempotency_key VARCHAR(80) NOT NULL UNIQUE,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(150) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  total_price DECIMAL(12,2) NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_orders_created (created_at),
  CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_orders_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;
