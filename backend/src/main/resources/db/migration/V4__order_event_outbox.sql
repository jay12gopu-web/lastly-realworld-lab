-- Outbox is written in the SAME SQL transaction as the order and stock reservation.
CREATE TABLE order_event_outbox (
  event_id VARCHAR(36) NOT NULL PRIMARY KEY,
  aggregate_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(60) NOT NULL,
  payload TEXT NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  sent_at DATETIME(6) NULL,
  INDEX idx_outbox_pending (status, created_at)
) ENGINE=InnoDB;
-- Single INSERT IGNORE protects this demo consumer from at-least-once duplicate delivery.
CREATE TABLE processed_order_events (
  event_id VARCHAR(36) NOT NULL PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(60) NOT NULL,
  processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
