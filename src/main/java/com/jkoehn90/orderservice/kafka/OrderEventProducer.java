package com.jkoehn90.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-placed";

    public void sendOrderPlacedEvent(Long orderId, Long userId, Double totalAmount) {
        String message = String.format(
                "{\"orderId\": %d, \"userId\": %d, \"totalAmount\": %.2f}",
                orderId, userId, totalAmount
        );

        kafkaTemplate.send(ORDER_TOPIC, message);
        log.info("Order placed event sent for orderId: {}", orderId);
    }
}
