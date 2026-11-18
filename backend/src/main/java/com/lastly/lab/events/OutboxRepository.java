package com.lastly.lab.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastly.lab.domain.CustomerOrder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public OutboxRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // Called inside OrderService.place's @Transactional method.
    public void saveOrderPlaced(CustomerOrder order) {
        String eventId = UUID.randomUUID().toString();
        String payload;
        try {
            payload = json.writeValueAsString(Map.of(
                "eventId", eventId,
                "orderId", order.getId(),
                "customerId", order.getCustomerId(),
                "productId", order.getProductId(),
                "quantity", order.getQuantity(),
                "occurredAt", Instant.now().toString(),
                "eventType", "OrderPlaced"));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize order event", ex);
        }
        jdbc.update("INSERT INTO order_event_outbox (event_id, aggregate_id, event_type, payload, status) VALUES (?, ?, ?, ?, 'PENDING')",
            eventId, order.getId(), "OrderPlaced", payload);
    }

    public List<OutboxEvent> pendingBatch() {
        return jdbc.query("SELECT event_id, aggregate_id, payload FROM order_event_outbox WHERE status = 'PENDING' ORDER BY created_at, event_id LIMIT 30",
            (rs, row) -> new OutboxEvent(rs.getString(1), rs.getString(2), rs.getString(3)));
    }

    public void markSent(String eventId) {
        jdbc.update("UPDATE order_event_outbox SET status = 'SENT', sent_at = UTC_TIMESTAMP(6), attempts = attempts + 1 WHERE event_id = ? AND status = 'PENDING'", eventId);
    }

    public void recordAttempt(String eventId) {
        jdbc.update("UPDATE order_event_outbox SET attempts = attempts + 1 WHERE event_id = ? AND status = 'PENDING'", eventId);
    }
}
