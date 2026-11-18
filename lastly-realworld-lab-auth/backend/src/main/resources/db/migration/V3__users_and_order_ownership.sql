CREATE TABLE user_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(32) NOT NULL,
  created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

-- Existing anonymous orders remain unowned and are not visible through secured order lookups.
ALTER TABLE customer_orders ADD COLUMN customer_id BIGINT NULL;
ALTER TABLE customer_orders ADD CONSTRAINT fk_orders_customer
  FOREIGN KEY (customer_id) REFERENCES user_accounts(id);
CREATE INDEX idx_customer_orders_customer ON customer_orders(customer_id);
