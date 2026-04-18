package com.retailpulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailpulse.dto.request.InventoryTransactionRequestDto;
import com.retailpulse.dto.request.InventoryTransactionUpdateRequestDto;
import com.retailpulse.dto.response.InventoryTransactionResponseDto;
import com.retailpulse.entity.InventoryTransaction;
import com.retailpulse.exception.GlobalExceptionHandler;
import com.retailpulse.service.InventoryTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryTransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @InjectMocks
    private InventoryTransactionController inventoryTransactionController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryTransactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCreateInventoryTransaction_usesRequestDtoAndMapsToEntity() throws Exception {
        InventoryTransactionRequestDto request = createRequest();
        InventoryTransactionResponseDto response = transactionResponse(UUID.randomUUID(), 10L, 4, 9.5, 101L, 201L);
        when(inventoryTransactionService.saveInventoryTransaction(org.mockito.ArgumentMatchers.any(InventoryTransaction.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/inventoryTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.costPricePerUnit").value(9.5))
                .andExpect(jsonPath("$.source").value(101))
                .andExpect(jsonPath("$.destination").value(201));

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionService).saveInventoryTransaction(captor.capture());

        InventoryTransaction mapped = captor.getValue();
        assertEquals(10L, mapped.getProductId());
        assertEquals(4, mapped.getQuantity());
        assertEquals(9.5, mapped.getCostPricePerUnit());
        assertEquals(101L, mapped.getSource());
        assertEquals(201L, mapped.getDestination());
    }

    @Test
    void testUpdateInventoryTransaction_usesUpdateDto() throws Exception {
        UUID transactionId = UUID.randomUUID();
        InventoryTransactionUpdateRequestDto request = updateRequest();
        InventoryTransactionResponseDto response = transactionResponse(transactionId, 15L, 7, 12.75, 101L, 301L);
        when(inventoryTransactionService.updateInventoryTransaction(org.mockito.ArgumentMatchers.eq(transactionId), org.mockito.ArgumentMatchers.any(InventoryTransactionUpdateRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/inventoryTransaction/{id}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(15))
                .andExpect(jsonPath("$.quantity").value(7))
                .andExpect(jsonPath("$.costPricePerUnit").value(12.75))
                .andExpect(jsonPath("$.source").value(101))
                .andExpect(jsonPath("$.destination").value(301));

        ArgumentCaptor<InventoryTransactionUpdateRequestDto> captor = ArgumentCaptor.forClass(InventoryTransactionUpdateRequestDto.class);
        verify(inventoryTransactionService).updateInventoryTransaction(org.mockito.ArgumentMatchers.eq(transactionId), captor.capture());

        InventoryTransactionUpdateRequestDto mapped = captor.getValue();
        assertEquals(15L, mapped.productId());
        assertNull(mapped.quantity());
        assertEquals(12.75, mapped.costPricePerUnit());
        assertNull(mapped.source());
        assertEquals(301L, mapped.destination());
    }

    @Test
    void testGetAllInventoryTransactionWithProduct_returnsPayload() throws Exception {
        when(inventoryTransactionService.getAllInventoryTransactionWithProduct()).thenReturn(List.of(
                new com.retailpulse.dto.response.InventoryTransactionProductResponseDto(
                        inventoryEntity(10L, 4, 9.5, 101L, 201L),
                        productEntity(10L, "Levis men jeans")
                )
        ));

        mockMvc.perform(get("/api/inventoryTransaction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].product.description").value("Levis men jeans"))
                .andExpect(jsonPath("$[0].inventoryTransaction.quantity").value(4));
    }

    @Test
    void testGetAllInventoryTransactionWithBusinessEntity_returnsPayload() throws Exception {
        when(inventoryTransactionService.getAllInventoryTransactionWithProductAndBusinessEntity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        new com.retailpulse.dto.response.InventoryTransactionProductBusinessEntityResponseDto(
                                inventoryEntity(10L, 4, 9.5, 101L, 201L),
                                productEntity(10L, "Levis men jeans"),
                                new com.retailpulse.dto.response.BusinessEntityResponseDto(101L, "Store A", "Yangon", "STORE", false, true),
                                new com.retailpulse.dto.response.BusinessEntityResponseDto(201L, "Store B", "Mandalay", "STORE", false, true)
                        )
                ));

        mockMvc.perform(post("/api/inventoryTransaction/withBusinessEntityDetails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDateTime\":\"2026-04-01T00:00:00Z\",\"endDateTime\":\"2026-04-02T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source.name").value("Store A"))
                .andExpect(jsonPath("$[0].destination.name").value("Store B"));
    }

    private com.retailpulse.entity.Product productEntity(Long id, String description) {
        com.retailpulse.entity.Product product = new com.retailpulse.entity.Product();
        product.setId(id);
        product.setDescription(description);
        return product;
    }

    private InventoryTransaction inventoryEntity(Long productId, int quantity, double costPricePerUnit, Long source, Long destination) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setProductId(productId);
        inventoryTransaction.setQuantity(quantity);
        inventoryTransaction.setCostPricePerUnit(costPricePerUnit);
        inventoryTransaction.setSource(source);
        inventoryTransaction.setDestination(destination);
        return inventoryTransaction;
    }

    private InventoryTransactionRequestDto createRequest() {
        return new InventoryTransactionRequestDto(10L, 4, 9.5, 101L, 201L);
    }

    private InventoryTransactionUpdateRequestDto updateRequest() {
        return new InventoryTransactionUpdateRequestDto(15L, null, 12.75, null, 301L);
    }

    private InventoryTransactionResponseDto transactionResponse(UUID id, Long productId, int quantity, double costPricePerUnit, Long source, Long destination) {
        return new InventoryTransactionResponseDto(
                id,
                productId,
                quantity,
                costPricePerUnit,
                source,
                destination,
                Instant.parse("2026-04-16T00:00:00Z")
        );
    }

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
