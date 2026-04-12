package com.example.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryOutcomeConsumer {

    private final OrderRepository repository;
    private final ObjectMapper objectMapper;

    public InventoryOutcomeConsumer(OrderRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory.v1", groupId = "orders")
    public void onMessage(String value) {
        try {
            InventoryOutcomeEvent event = objectMapper.readValue(value, InventoryOutcomeEvent.class);
            repository.findById(event.orderId())
                .flatMap(order -> {
                    OrderStatus newStatus = order.status();
                    if ("InventoryReserved".equals(event.type())) {
                        newStatus = OrderStatus.RESERVED;
                    } else if ("InventoryOutOfStock".equals(event.type())) {
                        newStatus = OrderStatus.FAILED;
                    }
                    Order updated = order.withStatus(newStatus);
                    return repository.save(updated)
                        .doOnNext(o -> System.out.println("Updated status for order: " + o.orderId() + " to: " + o.status()));
                })
                .subscribe();
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse event: " + value + " error: " + e.getMessage());
        }
    }
}
