package com.retailpulse.dto.request;

public record InventoryTransactionRequestDto(
        long productId,
        int quantity,
        double costPricePerUnit,
        long source,
        long destination
) {
}
