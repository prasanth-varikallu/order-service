package com.example.order;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
public record Order(@Id String orderId, String sku, int qty, OrderStatus status, @Version Integer version) {
    public Order(String orderId, String sku, int qty, OrderStatus status) {
        this(orderId, sku, qty, status, null);
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(orderId, sku, qty, newStatus, version);
    }
}
