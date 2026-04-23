package com.retailpulse;

import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.dto.Constants;
import com.retailpulse.dto.InventoryTransactionDetailsDto;
import com.retailpulse.dto.request.TimeSearchFilterRequestDto;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import com.retailpulse.dto.response.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.entity.AuditLogEntity;
import com.retailpulse.entity.InventoryTransaction;
import com.retailpulse.entity.Product;
import com.retailpulse.entity.SKUCounter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class MiscModelCoverageTest {

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            InventoryMicroservice.main(new String[]{"--spring.main.banner-mode=off"});

            springApplication.verify(() -> SpringApplication.run(InventoryMicroservice.class, new String[]{"--spring.main.banner-mode=off"}));
        }
    }

    @Test
    void modelTypes_exposeExpectedValues() {
        LocalDateTime now = LocalDateTime.now();
        AuditLogEntity auditLogEntity = new AuditLogEntity("admin", "CREATE", "SUCCESS", "127.0.0.1", now);
        SKUCounter skuCounter = new SKUCounter("product", 9L);
        ApplicationException applicationException = new ApplicationException("APP-9", "bad");
        TimeSearchFilterRequestDto filter = new TimeSearchFilterRequestDto(Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-02T00:00:00Z"));

        Product product = new Product();
        product.setId(1L);
        product.setDescription("Jeans");

        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setProductId(1L);
        inventoryTransaction.setSource(10L);
        inventoryTransaction.setDestination(20L);

        BusinessEntityResponseDto source = new BusinessEntityResponseDto(10L, "Store", "Yangon", "STORE", false, true);
        BusinessEntityResponseDto destination = new BusinessEntityResponseDto(20L, "Warehouse", "Mandalay", "WAREHOUSE", false, true);
        InventoryTransactionProductBusinessEntityResponseDto response =
                new InventoryTransactionProductBusinessEntityResponseDto(inventoryTransaction, product, source, destination);

        assertEquals("admin", auditLogEntity.getActor());
        assertEquals("127.0.0.1", auditLogEntity.getIpAddress());
        assertEquals(now, auditLogEntity.getTimestamp());
        assertEquals("product", skuCounter.getName());
        assertEquals(9L, skuCounter.getCounter());
        assertEquals("APP-9", applicationException.getErrorCode());
        assertEquals("bad", applicationException.getMessage());
        assertEquals("Authorization", Constants.AUTHORIZATION_HEADER);
        assertNotNull(new InventoryTransactionDetailsDto());
        assertEquals(Instant.parse("2026-04-01T00:00:00Z"), filter.startDateTime());
        assertEquals(source, response.source());
        assertEquals(destination, response.destination());
        assertEquals(product, response.product());
    }
}
