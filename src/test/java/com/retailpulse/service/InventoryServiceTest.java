package com.retailpulse.service;

import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.dto.response.InventoryResponseDto;
import com.retailpulse.entity.Inventory;
import com.retailpulse.repository.InventoryRepository;
import com.retailpulse.service.exception.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository; // Mocked dependency

    @Mock
    private BusinessEntityService businessEntityService;

    @InjectMocks
    private InventoryService inventoryService; // Service under test

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void testGetAllInventory() {
        // Arrange
        Inventory item1 = new Inventory();
        item1.setId(1L);
        item1.setProductId(101L);
        item1.setBusinessEntityId(201L);
        item1.setQuantity(50);

        Inventory item2 = new Inventory();
        item2.setId(2L);
        item2.setProductId(102L);
        item2.setBusinessEntityId(202L);
        item2.setQuantity(30);

        List<Inventory> mockInventories = Arrays.asList(item1, item2);

        when(inventoryRepository.findAll()).thenReturn(mockInventories);

        // Act
        List<InventoryResponseDto> result = inventoryService.getAllInventory();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals(101L, result.getFirst().productId());
        assertEquals(201L, result.getFirst().businessEntityId());
        assertEquals(50, result.getFirst().quantity());

        verify(inventoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryById() {
        // Arrange
        Long inventoryId = 1L;

        Inventory mockInventory = new Inventory();
        mockInventory.setId(inventoryId);
        mockInventory.setProductId(101L);
        mockInventory.setBusinessEntityId(201L);
        mockInventory.setQuantity(50);

        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(mockInventory));

        // Act
        InventoryResponseDto result = inventoryService.getInventoryById(inventoryId);

        // Assert
        assertNotNull(result);
        assertEquals(inventoryId, result.id());
        assertEquals(101L, result.productId());
        assertEquals(201L, result.businessEntityId());
        assertEquals(50, result.quantity());

        verify(inventoryRepository, times(1)).findById(inventoryId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryByProductId() {
        // Arrange
        Long productId = 101L;

        Inventory mockInventory = new Inventory();
        mockInventory.setId(1L);
        mockInventory.setProductId(productId);
        mockInventory.setBusinessEntityId(201L);
        mockInventory.setQuantity(50);

        List<Inventory> mockInventories = Collections.singletonList(mockInventory);

        when(inventoryRepository.findByProductId(productId)).thenReturn(mockInventories);

        // Act
        List<InventoryResponseDto> result = inventoryService.getInventoryByProductId(productId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(productId, result.getFirst().productId());
        assertEquals(201L, result.getFirst().businessEntityId());
        assertEquals(50, result.getFirst().quantity());

        verify(inventoryRepository, times(1)).findByProductId(productId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryByBusinessEntityId() {
        // Arrange
        Long businessEntityId = 201L;

        Inventory mockInventory = new Inventory();
        mockInventory.setId(1L);
        mockInventory.setProductId(101L);
        mockInventory.setBusinessEntityId(businessEntityId);
        mockInventory.setQuantity(50);

        List<Inventory> mockInventories = Collections.singletonList(mockInventory);

        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(true);
        when(inventoryRepository.findByBusinessEntityId(businessEntityId)).thenReturn(mockInventories);

        // Act
        List<InventoryResponseDto> result = inventoryService.getInventoryByBusinessEntityId(businessEntityId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(businessEntityId, result.getFirst().businessEntityId());
        assertEquals(101L, result.getFirst().productId());
        assertEquals(50, result.getFirst().quantity());

        verify(inventoryRepository, times(1)).findByBusinessEntityId(businessEntityId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testGetInventoryByProductIdAndBusinessEntityId() {
        // Arrange
        Long productId = 101L;
        Long businessEntityId = 201L;

        Inventory mockInventory = new Inventory();
        mockInventory.setId(1L);
        mockInventory.setProductId(productId);
        mockInventory.setBusinessEntityId(businessEntityId);
        mockInventory.setQuantity(50);

        when(inventoryRepository.findByProductIdAndBusinessEntityId(productId, businessEntityId))
                .thenReturn(Optional.of(mockInventory));
        when(businessEntityService.isValidBusinessEntity(businessEntityId)).thenReturn(true);

        // Act
        InventoryResponseDto result = inventoryService.getInventoryByProductIdAndBusinessEntityId(productId, businessEntityId);

        // Assert
        assertNotNull(result);
        assertEquals(productId, result.productId());
        assertEquals(businessEntityId, result.businessEntityId());
        assertEquals(50, result.quantity());

        verify(inventoryRepository, times(1)).findByProductIdAndBusinessEntityId(productId, businessEntityId);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    public void testInventoryContainsProduct_ReturnsTrueWhenProductExists() {
        Long productId = 1L;
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        when(inventoryRepository.findByProductId(productId)).thenReturn(List.of(inventory));

        boolean exists = inventoryService.inventoryContainsProduct(productId);
        assertTrue(exists, "Should return true when inventory exists for the given productId");
    }

    @Test
    public void testInventoryContainsProduct_ReturnsFalseWhenNoInventory() {
        Long productId = 2L;
        when(inventoryRepository.findByProductId(productId)).thenReturn(Collections.emptyList());

        boolean exists = inventoryService.inventoryContainsProduct(productId);
        assertFalse(exists, "Should return false when there is no inventory for the given productId");
    }

    @Test
    void testSaveInventory() {
        // Arrange
        Inventory inventoryToSave = new Inventory();
        inventoryToSave.setProductId(101L);
        inventoryToSave.setBusinessEntityId(201L);
        inventoryToSave.setQuantity(50);

        Inventory savedInventory = new Inventory();
        savedInventory.setId(1L);
        savedInventory.setProductId(101L);
        savedInventory.setBusinessEntityId(201L);
        savedInventory.setQuantity(50);

        when(inventoryRepository.save(inventoryToSave)).thenReturn(savedInventory);

        // Act
        Inventory result = inventoryService.saveInventory(inventoryToSave);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(101L, result.getProductId());
        assertEquals(201L, result.getBusinessEntityId());
        assertEquals(50, result.getQuantity());

        verify(inventoryRepository, times(1)).save(inventoryToSave);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testUpdateInventory() {
        // Arrange
        Long inventoryId = 1L;

        Inventory existingInventory = new Inventory();
        existingInventory.setId(inventoryId);
        existingInventory.setProductId(101L);
        existingInventory.setBusinessEntityId(201L);
        existingInventory.setQuantity(50);

        Inventory updatedDetails = new Inventory();
        updatedDetails.setProductId(102L);
        updatedDetails.setBusinessEntityId(202L);
        updatedDetails.setQuantity(60);

        Inventory updatedInventory = new Inventory();
        updatedInventory.setId(inventoryId);
        updatedInventory.setProductId(102L);
        updatedInventory.setBusinessEntityId(202L);
        updatedInventory.setQuantity(60);

        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);

        // Act
        Inventory result = inventoryService.updateInventory(inventoryId, updatedDetails);

        // Assert
        assertNotNull(result);
        assertEquals(inventoryId, result.getId());
        assertEquals(102L, result.getProductId());
        assertEquals(202L, result.getBusinessEntityId());
        assertEquals(60, result.getQuantity());

        verify(inventoryRepository, times(1)).findById(inventoryId);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testDeleteInventory() {
        // Arrange
        Long inventoryId = 1L;

        Inventory inventoryToDelete = new Inventory();
        inventoryToDelete.setId(inventoryId);
        inventoryToDelete.setProductId(101L);
        inventoryToDelete.setBusinessEntityId(201L);
        inventoryToDelete.setQuantity(50);

        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventoryToDelete));

        // Act
        Inventory result = inventoryService.deleteInventory(inventoryId);

        // Assert
        assertNotNull(result);
        assertEquals(inventoryId, result.getId());
        assertEquals(101L, result.getProductId());
        assertEquals(201L, result.getBusinessEntityId());
        assertEquals(50, result.getQuantity());

        verify(inventoryRepository, times(1)).findById(inventoryId);
        verify(inventoryRepository, times(1)).delete(inventoryToDelete);
        verifyNoMoreInteractions(inventoryRepository);
    }

    @Test
    void testSalesUpdateStocks_successfulDeduction() {
        long businessEntityId = 1L;
        long productId = 100L;

        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(productId);
        inventory.setBusinessEntityId(businessEntityId);
        inventory.setQuantity(50);

        InventoryUpdateRequestDto.InventoryItem item = new InventoryUpdateRequestDto.InventoryItem(productId, 10);
        InventoryUpdateRequestDto request = new InventoryUpdateRequestDto(businessEntityId, List.of(item));

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

        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(productId);
        inventory.setBusinessEntityId(businessEntityId);
        inventory.setQuantity(5);

        InventoryUpdateRequestDto.InventoryItem item = new InventoryUpdateRequestDto.InventoryItem(productId, 10);
        InventoryUpdateRequestDto request = new InventoryUpdateRequestDto(businessEntityId, List.of(item));

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
}