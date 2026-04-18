package com.retailpulse.dto.request;

public record InventoryTransactionUpdateRequestDto(
        Long productId,
        Integer quantity,
        Double costPricePerUnit,
        Long source,
        Long destination
) {
}
