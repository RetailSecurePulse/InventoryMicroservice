package com.retailpulse.dto.request;

public record ProductCreateRequestDto(
        String description,
        String category,
        String subcategory,
        String brand,
        String origin,
        String uom,
        String vendorCode,
        String barcode,
        Double rrp,
        Boolean active
) {
}
