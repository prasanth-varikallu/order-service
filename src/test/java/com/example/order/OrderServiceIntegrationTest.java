package com.example.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"orders.v1", "inventory.v1"}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class OrderServiceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderRepository repository;

    @Test
    public void testOrderLifecycle() throws Exception {
        String orderId = "order-123";
        OrderController.CreateOrder req = new OrderController.CreateOrder(orderId, "sku-1", 5);

        // 1. Create order
        webTestClient.post().uri("/orders")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Order.class)
            .consumeWith(result -> {
                Order order = result.getResponseBody();
                assertThat(order).isNotNull();
                assertThat(order.orderId()).isEqualTo(orderId);
                assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
            });

        // 2. Wait for order to be saved in repository
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Order order = repository.findById(orderId).block();
            assertThat(order).isNotNull();
            assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        });

        // 3. Simulate inventory reservation
        String inventoryEvent = """
            {"type":"InventoryReserved","orderId":"%s","sku":"sku-1","qty":5,"ts":"now"}
            """.formatted(orderId);
        kafkaTemplate.send("inventory.v1", orderId, inventoryEvent).get();

        // 4. Verify order status update
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Order order = repository.findById(orderId).block();
            assertThat(order).isNotNull();
            assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
        });
    }

    @Test
    public void testOrderOutOfStock() throws Exception {
        String orderId = "order-456";
        OrderController.CreateOrder req = new OrderController.CreateOrder(orderId, "sku-2", 100);

        // 1. Create order
        webTestClient.post().uri("/orders")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk();

        // 2. Simulate inventory out of stock
        String inventoryEvent = """
            {"type":"InventoryOutOfStock","orderId":"%s","sku":"sku-2","qty":100,"ts":"now"}
            """.formatted(orderId);
        kafkaTemplate.send("inventory.v1", orderId, inventoryEvent).get();

        // 3. Verify order status update
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Order order = repository.findById(orderId).block();
            assertThat(order).isNotNull();
            assertThat(order.status()).isEqualTo(OrderStatus.FAILED);
        });
    }
}
