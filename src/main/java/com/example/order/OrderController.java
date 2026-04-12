package com.example.order;

import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class OrderController {
  private final KafkaTemplate<String, String> kafka;

  public OrderController(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  @PostMapping("/orders")
  public Mono<String> create(@RequestBody CreateOrder req) {
    var event = """
      {"type":"OrderCreated","orderId":"%s","sku":"%s","qty":%d,"ts":"%s"}
      """.formatted(req.orderId(), req.sku(), req.qty(), Instant.now().toString());
    var response = kafka.send("orders.v1", req.orderId(), event);
    return Mono.fromFuture(response)
        .thenReturn(event);
  }

  public record CreateOrder(String orderId, String sku, int qty) {}
}

