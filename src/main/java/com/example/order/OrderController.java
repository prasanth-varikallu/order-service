package com.example.order;

import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class OrderController {
  private final KafkaTemplate<String, String> kafka;
  private final OrderRepository repository;

  public OrderController(KafkaTemplate<String, String> kafka, OrderRepository repository) {
    this.kafka = kafka;
    this.repository = repository;
  }

  @PostMapping("/orders")
  public Mono<Order> create(@RequestBody CreateOrder req) {
    var order = new Order(req.orderId(), req.sku(), req.qty(), OrderStatus.CREATED);
    var event = """
      {"type":"OrderCreated","orderId":"%s","sku":"%s","qty":%d,"ts":"%s"}
      """.formatted(req.orderId(), req.sku(), req.qty(), Instant.now().toString());
    
    return repository.save(order)
        .flatMap(savedOrder -> Mono.fromFuture(kafka.send("orders.v1", req.orderId(), event))
            .thenReturn(savedOrder));
  }

  @GetMapping("/orders/{orderId}")
  public Mono<Order> get(@PathVariable String orderId) {
    return repository.findById(orderId);
  }

  public record CreateOrder(String orderId, String sku, int qty) {}
}

