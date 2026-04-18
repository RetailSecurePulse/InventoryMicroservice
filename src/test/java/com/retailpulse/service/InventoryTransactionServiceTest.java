package com.retailpulse.service;

import com.retailpulse.dto.request.InventoryTransactionUpdateRequestDto;
import com.retailpulse.dto.response.*;
import com.retailpulse.entity.Inventory;
import com.retailpulse.entity.InventoryTransaction;
import com.retailpulse.entity.Product;
import com.retailpulse.repository.InventoryTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryTransactionServiceTest {

    @Mock
    private InventoryTransactionRepository mockInventoryTransactionRepository;

    @Mock
    private InventoryService mockInventoryService;

    @Mock
    private ProductService mockProductService;

    @Mock
    private BusinessEntityService mockBusinessEntityService;

    @InjectMocks
    private InventoryTransactionService inventoryTransactionService;

    @Test
    void testGetAllInventoryTransactionWithProduct() {
        when(mockInventoryTransactionRepository.findAllWithProduct()).thenReturn(Collections.singletonList(
                new InventoryTransactionProductResponseDto(productTransaction(1L), productEntity(1L, "Product A"))
        ));

        List<InventoryTransactionProductResponseDto> result = inventoryTransactionService.getAllInventoryTransactionWithProduct();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Product A", result.getFirst().product().getDescription());

        verify(mockInventoryTransactionRepository, times(1)).findAllWithProduct();
        verifyNoMoreInteractions(mockInventoryTransactionRepository);
    }

    @Test
    void testSaveInventoryTransaction_Successful() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, 10, 5.0);
        ProductResponseDto product = activeProduct();
        InventoryResponseDto sourceInventory = inventoryResponse(1L, 1L, 101L, 20, 100.0);
        InventoryResponseDto destinationInventory = inventoryResponse(2L, 1L, 201L, 30, 150.0);

        Inventory updatedSourceInventory = inventoryEntity(1L, 1L, 101L, 10, 50.0);
        Inventory updatedDestinationInventory = inventoryEntity(2L, 1L, 201L, 40, 200.0);

        when(mockProductService.getProductById(1L)).thenReturn(product);
        when(mockBusinessEntityService.isExternalBusinessEntity(101L)).thenReturn(false);
        when(mockBusinessEntityService.isExternalBusinessEntity(201L)).thenReturn(false);
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 101L)).thenReturn(sourceInventory);
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 201L)).thenReturn(destinationInventory);
        when(mockInventoryTransactionRepository.save(transaction)).thenReturn(transaction);

        InventoryTransactionResponseDto result = inventoryTransactionService.saveInventoryTransaction(transaction);

        assertTransactionResponse(result, 1L, 101L, 201L, 10, 5.0);

        verify(mockProductService, times(1)).getProductById(1L);
        verify(mockInventoryService, times(1)).getInventoryByProductIdAndBusinessEntityId(1L, 101L);
        verify(mockInventoryService, times(1)).getInventoryByProductIdAndBusinessEntityId(1L, 201L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(101L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(201L);

        ArgumentCaptor<Inventory> sourceCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(mockInventoryService, times(1)).updateInventory(eq(1L), sourceCaptor.capture());
        Inventory actualUpdatedSource = sourceCaptor.getValue();
        assertEquals(updatedSourceInventory.getId(), actualUpdatedSource.getId());
        assertEquals(updatedSourceInventory.getProductId(), actualUpdatedSource.getProductId());
        assertEquals(updatedSourceInventory.getBusinessEntityId(), actualUpdatedSource.getBusinessEntityId());
        assertEquals(updatedSourceInventory.getQuantity(), actualUpdatedSource.getQuantity());
        assertEquals(updatedSourceInventory.getTotalCostPrice(), actualUpdatedSource.getTotalCostPrice());

        ArgumentCaptor<Inventory> destinationCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(mockInventoryService, times(1)).updateInventory(eq(2L), destinationCaptor.capture());
        Inventory actualUpdatedDestination = destinationCaptor.getValue();
        assertEquals(updatedDestinationInventory.getId(), actualUpdatedDestination.getId());
        assertEquals(updatedDestinationInventory.getProductId(), actualUpdatedDestination.getProductId());
        assertEquals(updatedDestinationInventory.getBusinessEntityId(), actualUpdatedDestination.getBusinessEntityId());
        assertEquals(updatedDestinationInventory.getQuantity(), actualUpdatedDestination.getQuantity());
        assertEquals(updatedDestinationInventory.getTotalCostPrice(), actualUpdatedDestination.getTotalCostPrice());

        verify(mockInventoryTransactionRepository, times(1)).save(transaction);
    }

    @Test
    void testSaveInventoryTransaction_InsufficientSourceQuantity() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, 30, 5.0);
        ProductResponseDto product = activeProduct();
        InventoryResponseDto sourceInventory = inventoryResponse(1L, 1L, 101L, 20, 100.0);

        when(mockProductService.getProductById(1L)).thenReturn(product);
        when(mockBusinessEntityService.isExternalBusinessEntity(101L)).thenReturn(false);
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 101L)).thenReturn(sourceInventory);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> inventoryTransactionService.saveInventoryTransaction(transaction));

        assertEquals(
                "Not enough quantity in source inventory for product id: 1 and source id: 101. Available: 20, required: 30",
                exception.getMessage()
        );

        verify(mockProductService, times(1)).getProductById(1L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(101L);
        verify(mockBusinessEntityService, never()).isExternalBusinessEntity(201L);
        verify(mockInventoryService, times(1)).getInventoryByProductIdAndBusinessEntityId(1L, 101L);
        verify(mockInventoryService, never()).updateInventory(anyLong(), any(Inventory.class));
        verify(mockInventoryTransactionRepository, never()).save(any());
    }

    @Test
    void testSaveInventoryTransaction_InvalidProduct() {
        InventoryTransaction transaction = transaction(999L, 101L, 201L, 10, 5.0);

        when(mockProductService.getProductById(999L)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> inventoryTransactionService.saveInventoryTransaction(transaction));

        assertEquals("Product not found for product id: 999", exception.getMessage());

        verify(mockProductService, times(1)).getProductById(999L);
        verifyNoInteractions(mockBusinessEntityService, mockInventoryService, mockInventoryTransactionRepository);
    }

    @Test
    void testSaveInventoryTransaction_SourceSameAsDestination() {
        InventoryTransaction transaction = transaction(1L, 101L, 101L, 10, 5.0);
        ProductResponseDto product = activeProduct();

        when(mockProductService.getProductById(1L)).thenReturn(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> inventoryTransactionService.saveInventoryTransaction(transaction));

        assertEquals("Source and Destination cannot be the same", exception.getMessage());

        verify(mockProductService, times(1)).getProductById(1L);
        verifyNoMoreInteractions(mockProductService);
    }

    @Test
    void testSaveInventoryTransaction_NegativeQuantity() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, -5, 5.0);
        ProductResponseDto product = activeProduct();

        when(mockProductService.getProductById(1L)).thenReturn(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> inventoryTransactionService.saveInventoryTransaction(transaction));

        assertEquals("Quantity cannot be negative or zero", exception.getMessage());

        verify(mockProductService, times(1)).getProductById(1L);
        verifyNoMoreInteractions(mockProductService);
    }

    @Test
    void testSaveInventoryTransaction_SourceExternal() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, 10, 5.0);
        ProductResponseDto product = activeProduct();
        InventoryResponseDto destinationInventoryResponse = inventoryResponse(2L, 1L, 201L, 50, 100.0);

        when(mockProductService.getProductById(1L)).thenReturn(product);
        when(mockBusinessEntityService.isExternalBusinessEntity(101L)).thenReturn(true);  // source = external
        when(mockBusinessEntityService.isExternalBusinessEntity(201L)).thenReturn(false); // destination = internal
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 201L))
                .thenReturn(destinationInventoryResponse);
        when(mockInventoryTransactionRepository.save(transaction)).thenReturn(transaction);

        InventoryTransactionResponseDto result = inventoryTransactionService.saveInventoryTransaction(transaction);

        assertTransactionResponse(result, 1L, 101L, 201L, 10, 5.0);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(mockInventoryService, times(1)).updateInventory(eq(2L), captor.capture());

        Inventory updated = captor.getValue();
        assertEquals(2L, updated.getId());
        assertEquals(1L, updated.getProductId());
        assertEquals(201L, updated.getBusinessEntityId());
        assertEquals(60, updated.getQuantity());         // 50 + 10
        assertEquals(150.0, updated.getTotalCostPrice()); // 100.0 + (10 * 5.0)

        verify(mockInventoryService, never())
                .getInventoryByProductIdAndBusinessEntityId(eq(1L), eq(101L));
        verify(mockInventoryService, never())
                .updateInventory(eq(1L), any(Inventory.class));
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(101L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(201L);

        verify(mockInventoryTransactionRepository, times(1)).save(transaction);
    }


    @Test
    void testSaveInventoryTransaction_DestinationExternal() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, 10, 5.0);
        ProductResponseDto product = activeProduct();
        InventoryResponseDto sourceInventory = inventoryResponse(1L, 1L, 101L, 20, 100.0);

        when(mockProductService.getProductById(1L)).thenReturn(product);
        when(mockBusinessEntityService.isExternalBusinessEntity(101L)).thenReturn(false); // source = internal
        when(mockBusinessEntityService.isExternalBusinessEntity(201L)).thenReturn(true);  // destination = external
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 101L))
                .thenReturn(sourceInventory);
        when(mockInventoryTransactionRepository.save(transaction)).thenReturn(transaction);

        InventoryTransactionResponseDto result = inventoryTransactionService.saveInventoryTransaction(transaction);

        assertTransactionResponse(result, 1L, 101L, 201L, 10, 5.0);

        // Verify source inventory update
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(mockInventoryService, times(1)).updateInventory(eq(1L), captor.capture());

        Inventory updated = captor.getValue();
        assertEquals(1L, updated.getId());
        assertEquals(1L, updated.getProductId());
        assertEquals(101L, updated.getBusinessEntityId());
        assertEquals(10, updated.getQuantity());        // 20 - 10
        assertEquals(50.0, updated.getTotalCostPrice()); // 100.0 - (10 * 5.0)

        verify(mockInventoryService, never())
                .updateInventory(eq(201L), any(Inventory.class));
        verify(mockInventoryService, never())
                .saveInventory(any(Inventory.class));
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(101L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(201L);

        verify(mockInventoryTransactionRepository, times(1)).save(transaction);
    }

    @Test
    void testSaveInventoryTransaction_BothExternal() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, 10, 5.0);
        ProductResponseDto product = activeProduct();

        when(mockProductService.getProductById(1L)).thenReturn(product);
        when(mockBusinessEntityService.isExternalBusinessEntity(101L)).thenReturn(true);  // source = external
        when(mockBusinessEntityService.isExternalBusinessEntity(201L)).thenReturn(true);  // destination = external
        when(mockInventoryTransactionRepository.save(transaction)).thenReturn(transaction);

        InventoryTransactionResponseDto result = inventoryTransactionService.saveInventoryTransaction(transaction);

        assertTransactionResponse(result, 1L, 101L, 201L, 10, 5.0);

        verify(mockInventoryService, never())
                .getInventoryByProductIdAndBusinessEntityId(anyLong(), anyLong());
        verify(mockInventoryService, never())
                .updateInventory(anyLong(), any(Inventory.class));
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(101L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(201L);

        verify(mockProductService, times(1)).getProductById(1L);
        verify(mockInventoryTransactionRepository, times(1)).save(transaction);
    }


    @Test
    void testSaveInventoryTransaction_NewDestinationInventory() {
        InventoryTransaction transaction = transaction(1L, 101L, 201L, 10, 5.0);
        ProductResponseDto product = activeProduct();
        Inventory sourceInventory = inventoryEntity(1L, 1L, 101L, 20, 100.0);
        InventoryResponseDto sourceInventoryResponse = inventoryResponse(1L, 1L, 101L, 20, 100.0);
        Inventory updatedSourceInventory = inventoryEntity(1L, 1L, 101L, 10, 50.0);

        when(mockProductService.getProductById(1L)).thenReturn(product);
        when(mockBusinessEntityService.isExternalBusinessEntity(101L)).thenReturn(false);
        when(mockBusinessEntityService.isExternalBusinessEntity(201L)).thenReturn(false);
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 101L))
                .thenReturn(sourceInventoryResponse);
        when(mockInventoryService.getInventoryByProductIdAndBusinessEntityId(1L, 201L))
                .thenReturn(null);
        when(mockInventoryService.saveInventory(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(mockInventoryService.updateInventory(1L, updatedSourceInventory))
                .thenReturn(updatedSourceInventory);
        when(mockInventoryTransactionRepository.save(transaction)).thenReturn(transaction);

        InventoryTransactionResponseDto result = inventoryTransactionService.saveInventoryTransaction(transaction);

        assertTransactionResponse(result, 1L, 101L, 201L, 10, 5.0);

        verify(mockProductService, times(1)).getProductById(1L);
        verify(mockInventoryService, times(1))
                .getInventoryByProductIdAndBusinessEntityId(1L, 101L);
        verify(mockInventoryService, times(1))
                .updateInventory(1L, updatedSourceInventory);
        verify(mockInventoryService, times(1))
                .getInventoryByProductIdAndBusinessEntityId(1L, 201L);
        verify(mockInventoryService, times(1))
                .saveInventory(argThat(inv ->
                        inv.getProductId() == 1L &&
                        inv.getBusinessEntityId() == 201L &&
                        inv.getQuantity() == 10 &&
                        inv.getTotalCostPrice() == 5.0 * 10));
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(101L);
        verify(mockBusinessEntityService, times(1)).isExternalBusinessEntity(201L);
        verify(mockInventoryTransactionRepository, times(1)).save(transaction);
    }

    @Test
    void testUpdateInventoryTransaction_partialUpdatePreservesExistingValues() {
        UUID transactionId = UUID.randomUUID();
        Instant insertedAt = Instant.parse("2026-04-16T00:00:00Z");

        InventoryTransaction existingTransaction = transaction(1L, 101L, 201L, 7, 5.5);
        existingTransaction.setId(transactionId);
        existingTransaction.setInsertedAt(insertedAt);

        InventoryTransactionUpdateRequestDto request = new InventoryTransactionUpdateRequestDto(2L, null, 9.75, null, 301L);

        when(mockInventoryTransactionRepository.findById(transactionId)).thenReturn(java.util.Optional.of(existingTransaction));
        when(mockInventoryTransactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransactionResponseDto result = inventoryTransactionService.updateInventoryTransaction(transactionId, request);

        assertEquals(transactionId, result.id());
        assertEquals(2L, result.productId());
        assertEquals(7, result.quantity());
        assertEquals(9.75, result.costPricePerUnit());
        assertEquals(101L, result.source());
        assertEquals(301L, result.destination());
        assertEquals(insertedAt, result.insertedAt());

        verify(mockInventoryTransactionRepository, times(1)).findById(transactionId);
        verify(mockInventoryTransactionRepository, times(1)).save(existingTransaction);
    }

    private InventoryTransaction transaction(Long productId, Long source, Long destination, int quantity, double costPricePerUnit) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(productId);
        transaction.setSource(source);
        transaction.setDestination(destination);
        transaction.setQuantity(quantity);
        transaction.setCostPricePerUnit(costPricePerUnit);
        return transaction;
    }

    private InventoryTransaction productTransaction(Long productId) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(productId);
        return transaction;
    }

    private Product productEntity(Long id, String description) {
        Product product = new Product();
        product.setId(id);
        product.setDescription(description);
        return product;
    }

    private ProductResponseDto activeProduct() {
        return new ProductResponseDto(
                1L, "LEV-M-001", "Levis men jeans", "Jeans", "Men",
                "Levis", "USA", "each", "LEV", "LEV-B-001", 100.00, true
        );
    }

    private InventoryResponseDto inventoryResponse(Long id, Long productId, Long businessEntityId, int quantity, double totalCostPrice) {
        return new InventoryResponseDto(id, productId, businessEntityId, quantity, totalCostPrice);
    }

    private Inventory inventoryEntity(Long id, Long productId, Long businessEntityId, int quantity, double totalCostPrice) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setProductId(productId);
        inventory.setBusinessEntityId(businessEntityId);
        inventory.setQuantity(quantity);
        inventory.setTotalCostPrice(totalCostPrice);
        return inventory;
    }

    private void assertTransactionResponse(InventoryTransactionResponseDto result, long productId, long source, long destination, int quantity, double costPricePerUnit) {
        assertNotNull(result);
        assertEquals(productId, result.productId());
        assertEquals(source, result.source());
        assertEquals(destination, result.destination());
        assertEquals(quantity, result.quantity());
        assertEquals(costPricePerUnit, result.costPricePerUnit(), 0.01);
    }
}
