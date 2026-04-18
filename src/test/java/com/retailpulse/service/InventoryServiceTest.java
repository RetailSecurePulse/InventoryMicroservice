package com.retailpulse.service;

import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.dto.response.InventoryResponseDto;
import com.retailpulse.entity.Inventory;
import com.retailpulse.repository.InventoryRepository;
import com.retailpulse.service.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BusinessEntityService businessEntityService;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void testGetAllInventory() {
        when(inventoryRepository.findAll()).thenReturn(List.of(
                inventory(1L, 101L, 201L, 50),
                inventory(2L, 102L, 202L, 30)
        ));

        List<InventoryResponseDto> result = inventoryService.getAllInventory();

        assertEquals(2, result.size());
        assertInventoryResponse(result.getFirst(), 1L, 101L, 201L, 50);
        verify(inventoryRepository).findAll();
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryById() {
        Long inventoryId = 1L;
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory(inventoryId, 101L, 201L, 50)));

        InventoryResponseDto result = inventoryService.getInventoryById(inventoryId);

        assertInventoryResponse(result, inventoryId, 101L, 201L, 50);
        verify(inventoryRepository).findById(inventoryId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryByProductId() {
        Long productId = 101L;
        when(inventoryRepository.findByProductId(productId)).thenReturn(List.of(inventory(1L, productId, 201L, 50)));

        List<InventoryResponseDto> result = inventoryService.getInventoryByProductId(productId);

        assertEquals(1, result.size());
        assertInventoryResponse(result.getFirst(), 1L, productId, 201L, 50);
        verify(inventoryRepository).findByProductId(productId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryByBusinessEntityId() {
        Long businessEntityId = 201L;
        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(true);
        when(inventoryRepository.findByBusinessEntityId(businessEntityId))
                .thenReturn(List.of(inventory(1L, 101L, businessEntityId, 50)));

        List<InventoryResponseDto> result = inventoryService.getInventoryByBusinessEntityId(businessEntityId);

        assertEquals(1, result.size());
        assertInventoryResponse(result.getFirst(), 1L, 101L, businessEntityId, 50);
        verify(inventoryRepository).findByBusinessEntityId(businessEntityId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryByProductIdAndBusinessEntityId() {
        Long productId = 101L;
        Long businessEntityId = 201L;
        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(true);
        when(inventoryRepository.findByProductIdAndBusinessEntityId(productId, businessEntityId))
                .thenReturn(Optional.of(inventory(1L, productId, businessEntityId, 50)));

        InventoryResponseDto result = inventoryService.getInventoryByProductIdAndBusinessEntityId(productId, businessEntityId);

        assertInventoryResponse(result, 1L, productId, businessEntityId, 50);
        verify(inventoryRepository).findByProductIdAndBusinessEntityId(productId, businessEntityId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testInventoryContainsProduct_ReturnsTrueWhenProductExists() {
        Long productId = 1L;
        when(inventoryRepository.findByProductId(productId)).thenReturn(List.of(inventory(null, productId, 201L, 10)));

        boolean exists = inventoryService.inventoryContainsProduct(productId);

        assertTrue(exists);
    }

    @Test
    void testInventoryContainsProduct_ReturnsFalseWhenNoInventory() {
        Long productId = 2L;
        when(inventoryRepository.findByProductId(productId)).thenReturn(List.of());

        boolean exists = inventoryService.inventoryContainsProduct(productId);

        assertFalse(exists);
    }

    @Test
    void testSaveInventory() {
        Inventory inventoryToSave = inventory(null, 101L, 201L, 50);
        Inventory savedInventory = inventory(1L, 101L, 201L, 50);

        when(inventoryRepository.save(inventoryToSave)).thenReturn(savedInventory);

        Inventory result = inventoryService.saveInventory(inventoryToSave);

        assertInventory(result, 1L, 101L, 201L, 50);
        verify(inventoryRepository).save(inventoryToSave);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testUpdateInventory() {
        Long inventoryId = 1L;
        Inventory existingInventory = inventory(inventoryId, 101L, 201L, 50);
        Inventory updatedDetails = inventory(null, 102L, 202L, 60);
        Inventory updatedInventory = inventory(inventoryId, 102L, 202L, 60);

        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);

        Inventory result = inventoryService.updateInventory(inventoryId, updatedDetails);

        assertInventory(result, inventoryId, 102L, 202L, 60);
        verify(inventoryRepository).findById(inventoryId);
        verify(inventoryRepository).save(any(Inventory.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testDeleteInventory() {
        Long inventoryId = 1L;
        Inventory inventoryToDelete = inventory(inventoryId, 101L, 201L, 50);
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventoryToDelete));

        Inventory result = inventoryService.deleteInventory(inventoryId);

        assertInventory(result, inventoryId, 101L, 201L, 50);
        verify(inventoryRepository).findById(inventoryId);
        verify(inventoryRepository).delete(inventoryToDelete);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testSalesUpdateStocks_successfulDeduction() {
        long businessEntityId = 1L;
        long productId = 100L;
        Inventory inventory = inventory(1L, productId, businessEntityId, 50);
        InventoryUpdateRequestDto request = salesUpdateRequest(businessEntityId, productId, 10);

        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(true);
        when(inventoryRepository.findByProductIdAndBusinessEntityId(productId, businessEntityId)).thenReturn(Optional.of(inventory));

        inventoryService.salesUpdateStocks(request);

        assertEquals(40, inventory.getQuantity());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testSalesUpdateStocks_insufficientStock_throwsException() {
        long businessEntityId = 1L;
        long productId = 100L;
        Inventory inventory = inventory(1L, productId, businessEntityId, 5);
        InventoryUpdateRequestDto request = salesUpdateRequest(businessEntityId, productId, 10);

        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(true);
        when(inventoryRepository.findByProductIdAndBusinessEntityId(productId, businessEntityId)).thenReturn(Optional.of(inventory));

        BusinessException ex = assertThrows(BusinessException.class, () -> inventoryService.salesUpdateStocks(request));

        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void testSalesUpdateStocks_invalidBusinessEntity_throwsException() {
        long businessEntityId = 99L;
        InventoryUpdateRequestDto request = new InventoryUpdateRequestDto(businessEntityId, List.of());
        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> inventoryService.salesUpdateStocks(request));

        assertEquals("INVALID_BUSINESS_ENTITY", ex.getCode());
    }

    private Inventory inventory(Long id, Long productId, Long businessEntityId, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setProductId(productId);
        inventory.setBusinessEntityId(businessEntityId);
        inventory.setQuantity(quantity);
        return inventory;
    }

    private InventoryUpdateRequestDto salesUpdateRequest(long businessEntityId, long productId, int quantity) {
        return new InventoryUpdateRequestDto(
                businessEntityId,
                List.of(new InventoryUpdateRequestDto.InventoryItem(productId, quantity))
        );
    }

    private void assertInventory(Inventory inventory, Long id, Long productId, Long businessEntityId, int quantity) {
        assertNotNull(inventory);
        assertEquals(id, inventory.getId());
        assertEquals(productId, inventory.getProductId());
        assertEquals(businessEntityId, inventory.getBusinessEntityId());
        assertEquals(quantity, inventory.getQuantity());
    }

    private void assertInventoryResponse(InventoryResponseDto response, Long id, Long productId, Long businessEntityId, int quantity) {
        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(productId, response.productId());
        assertEquals(businessEntityId, response.businessEntityId());
        assertEquals(quantity, response.quantity());
    }
}
