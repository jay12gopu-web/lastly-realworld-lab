package com.lastly.lab.events;

public record OutboxEvent(String eventId, String orderId, String payload) {}
