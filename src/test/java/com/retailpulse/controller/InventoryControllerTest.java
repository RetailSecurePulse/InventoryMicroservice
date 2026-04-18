package com.retailpulse.controller;

import com.retailpulse.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.service.InventoryService;
import com.retailpulse.service.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler()) 
                .build();
    }

    @Test
    void testSalesUpdateStocks_success() throws Exception {
        InventoryUpdateRequestDto request = salesUpdateRequest(5);

        mockMvc.perform(post("/api/inventory/salesUpdate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk());

        verify(inventoryService).salesUpdateStocks(request);
    }

    @Test
    void testSalesUpdateStocks_businessException() throws Exception {
        InventoryUpdateRequestDto request = salesUpdateRequest(999);

        doThrow(new BusinessException("INSUFFICIENT_STOCK", "Not enough stock"))
                .when(inventoryService).salesUpdateStocks(Mockito.any());

        mockMvc.perform(post("/api/inventory/salesUpdate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Not enough stock"));
    }

    @Test
    void testGetAllInventories_returnsPayload() throws Exception {
        when(inventoryService.getAllInventory()).thenReturn(List.of(
                new com.retailpulse.dto.response.InventoryResponseDto(1L, 100L, 200L, 50, 75.0)
        ));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(100))
                .andExpect(jsonPath("$[0].quantity").value(50));
    }

    @Test
    void testGetInventoryById_returnsPayload() throws Exception {
        when(inventoryService.getInventoryById(1L))
                .thenReturn(new com.retailpulse.dto.response.InventoryResponseDto(1L, 100L, 200L, 50, 75.0));

        mockMvc.perform(get("/api/inventory/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessEntityId").value(200));
    }

    @Test
    void testGetInventoryByProductId_returnsPayload() throws Exception {
        when(inventoryService.getInventoryByProductId(100L)).thenReturn(List.of(
                new com.retailpulse.dto.response.InventoryResponseDto(1L, 100L, 200L, 50, 75.0)
        ));

        mockMvc.perform(get("/api/inventory/productId/{id}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(100));
    }

    @Test
    void testGetInventoryByBusinessEntityId_returnsPayload() throws Exception {
        when(inventoryService.getInventoryByBusinessEntityId(200L)).thenReturn(List.of(
                new com.retailpulse.dto.response.InventoryResponseDto(1L, 100L, 200L, 50, 75.0)
        ));

        mockMvc.perform(get("/api/inventory/businessEntityId/{businessEntityId}", 200L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessEntityId").value(200));
    }

    @Test
    void testGetInventoryByProductIdAndBusinessEntityId_returnsPayload() throws Exception {
        when(inventoryService.getInventoryByProductIdAndBusinessEntityId(100L, 200L))
                .thenReturn(new com.retailpulse.dto.response.InventoryResponseDto(1L, 100L, 200L, 50, 75.0));

        mockMvc.perform(get("/api/inventory/productId/{productId}/businessEntityId/{businessEntityId}", 100L, 200L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.businessEntityId").value(200));
    }

    private InventoryUpdateRequestDto salesUpdateRequest(int quantity) {
        return new InventoryUpdateRequestDto(1L, List.of(new InventoryUpdateRequestDto.InventoryItem(100L, quantity)));
    }

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
