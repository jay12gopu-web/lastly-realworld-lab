package com.lastly.lab.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {
    @Mock OutboxRepository outbox;
    @Mock KafkaTemplate<String, String> kafka;

    @Test void brokerAcknowledgementMarksOutboxSent() {
        var e = new OutboxEvent("event-1", "order-1", "{\"eventId\":\"event-1\"}");
        when(outbox.pendingBatch()).thenReturn(List.of(e));
        when(kafka.send("lastly.order.placed", "order-1", e.payload())).thenReturn(CompletableFuture.completedFuture(null));
        new OrderEventPublisher(outbox, kafka, "lastly.order.placed").publishPending();
        verify(outbox).markSent("event-1");
        verify(outbox, never()).recordAttempt(anyString());
    }

    @Test void kafkaFailureKeepsOutboxPendingForRetry() {
        var e = new OutboxEvent("event-2", "order-2", "{}");
        when(outbox.pendingBatch()).thenReturn(List.of(e));
        when(kafka.send("lastly.order.placed", "order-2", e.payload()))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker offline")));
        new OrderEventPublisher(outbox, kafka, "lastly.order.placed").publishPending();
        verify(outbox).recordAttempt("event-2");
        verify(outbox, never()).markSent(anyString());
    }
}
