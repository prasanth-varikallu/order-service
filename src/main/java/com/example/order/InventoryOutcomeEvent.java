package com.example.order;

public record InventoryOutcomeEvent(
    String type,
    String orderId,
    String sku,
    int qty,
    String ts
) {}
