package com.lastly.lab.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public OrderEventConsumer(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @KafkaListener(topics = "${app.events.topic:lastly.order.placed}", groupId = "${spring.kafka.consumer.group-id:lastly-order-audit}")
    @Transactional
    public void onMessage(String message) throws JsonProcessingException {
        JsonNode event = json.readTree(message);
        String eventId = required(event, "eventId");
        String orderId = required(event, "orderId");
        String eventType = required(event, "eventType");
        // INSERT IGNORE is atomic; a repeated Kafka message has no second side effect.
        int inserted = jdbc.update("INSERT IGNORE INTO processed_order_events (event_id, order_id, event_type) VALUES (?, ?, ?)",
            eventId, orderId, eventType);
        if (inserted == 1) log.info("EVENT_PROCESSED eventId={} orderId={}", eventId, orderId);
        else log.info("EVENT_DUPLICATE_IGNORED eventId={}", eventId);
    }

    private String required(JsonNode event, String field) {
        if (!event.hasNonNull(field) || event.get(field).asText().isBlank())
            throw new IllegalArgumentException("Invalid event missing " + field);
        return event.get(field).asText();
    }
}
