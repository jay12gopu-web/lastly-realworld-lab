package com.lastly.lab.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KafkaEventConfig {
    @Bean
    @ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
    NewTopic orderPlacedTopic(@Value("${app.events.topic:lastly.order.placed}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
