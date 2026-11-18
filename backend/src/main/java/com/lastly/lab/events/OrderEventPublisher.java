package com.lastly.lab.events;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OrderEventPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka,
            @Value("${app.events.topic:lastly.order.placed}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(initialDelay = 4000, fixedDelayString = "${app.events.poll-ms:3000}")
    public void publishPending() {
        for (OutboxEvent event : outbox.pendingBatch()) {
            try {
                // Wait for Kafka broker ack before marking the SQL row as sent.
                kafka.send(topic, event.orderId(), event.payload()).get(5, TimeUnit.SECONDS);
                outbox.markSent(event.eventId());
                log.info("OUTBOX_SENT eventId={} orderId={}", event.eventId(), event.orderId());
            } catch (Exception ex) {
                outbox.recordAttempt(event.eventId());
                log.warn("OUTBOX_RETRY eventId={} cause={}", event.eventId(), ex.toString());
                // Broker is probably unavailable; don't hammer 30 failing sends every cycle.
                break;
            }
        }
    }
}
