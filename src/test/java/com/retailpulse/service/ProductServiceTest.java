package com.retailpulse.service;

import com.retailpulse.dto.request.ProductUpdateRequestDto;
import com.retailpulse.dto.response.ProductResponseDto;
import com.retailpulse.entity.Product;
import com.retailpulse.repository.ProductRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SKUGeneratorService skuGeneratorService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ProductService productService;

    @Test
    void testGetAllProducts_Success() {
        when(productRepository.findAll()).thenReturn(List.of(product(1L, null, null, 0, true)));

        List<ProductResponseDto> result = productService.getAllProducts();

        assertFalse(result.isEmpty());
        assertEquals(1L, result.getFirst().id());
        verify(productRepository).findAll();
    }

    @Test
    void testGetProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, null, null, 0, true)));

        ProductResponseDto result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void testGetProductById_NotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        com.retailpulse.service.exception.BusinessException exception = assertThrows(
                com.retailpulse.service.exception.BusinessException.class,
                () -> productService.getProductById(99L)
        );

        assertEquals("PRODUCT_BY_ID_NOT_FOUND", exception.getCode());
    }

    @Test
    void testGetProductBySKU_Success() {
        when(productRepository.findBySku("RP12345")).thenReturn(Optional.of(product(1L, "RP12345", null, 0, true)));

        ProductResponseDto result = productService.getProductBySKU("RP12345");

        assertNotNull(result);
        assertEquals("RP12345", result.sku());
    }

    @Test
    void testGetProductBySKU_NotFound() {
        when(productRepository.findBySku("RP99999")).thenReturn(Optional.empty());

        com.retailpulse.service.exception.BusinessException exception = assertThrows(
                com.retailpulse.service.exception.BusinessException.class,
                () -> productService.getProductBySKU("RP99999")
        );

        assertEquals("PRODUCT_BY_SKU_NOT_FOUND", exception.getCode());
    }

    @Test
    void testSaveProduct_Success() {
        Product product = product(null, null, null, 0, true);
        when(skuGeneratorService.generateSKU()).thenReturn("RP12345");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product savedProduct = invocation.getArgument(0);
            savedProduct.setId(1L);
            return savedProduct;
        });

        ProductResponseDto result = productService.saveProduct(product);

        assertEquals("RP12345", result.sku());
        assertNotNull(result.id());
    }

    @Test
    void testUpdateProduct_Success() {
        Product existingProduct = product(1L, "12345", "Old Description", 10, true);
        ProductUpdateRequestDto updatedProduct = updateRequest("New Description", -1.0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDto result = productService.updateProduct(1L, updatedProduct);

        assertEquals("12345", result.sku());
        assertEquals("New Description", result.description());
        assertEquals(10, result.rrp());
        assertTrue(result.active());
    }

    @Test
    void testSaveProduct_NegativeRrp() {
        Product product = product(null, null, null, -5, true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> productService.saveProduct(product));

        assertEquals("Recommended retail price cannot be negative", exception.getMessage());
        verifyNoInteractions(skuGeneratorService);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_NotFound() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.updateProduct(productId, updateRequest("New Description", 20.0)));

        assertEquals("Product not found with id: " + productId, exception.getMessage());
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_DeletedProduct() {
        Long productId = 1L;
        Product existingProduct = product(productId, "12345", "Old Description", 10, false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.updateProduct(productId, updateRequest("New Description", 20.0)));

        assertEquals("Cannot update a deleted product with id: " + productId, exception.getMessage());
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProductDoesNotChangeIsActive_Success() {
        Product existingProduct = product(1L, "12345", "Old Description", 10, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDto result = productService.updateProduct(1L, updateRequest("New Description", null));

        assertEquals("12345", result.sku());
        assertEquals("New Description", result.description());
        assertTrue(result.active());
    }

    @Test
    void testUpdateProduct_ignoresEmptyStringsAndNegativeRrp() {
        Product existingProduct = product(1L, "12345", "Old Description", 10, true);
        existingProduct.setCategory("Old Category");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductUpdateRequestDto request = new ProductUpdateRequestDto("", "", "", "", "", "", "", "", -5.0);
        ProductResponseDto result = productService.updateProduct(1L, request);

        assertEquals("Old Description", result.description());
        assertEquals("Old Category", result.category());
        assertEquals(10, result.rrp());
    }

    @Test
    void testDeleteProduct_Success() {
        Product product = product(1L, null, null, 0, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.inventoryContainsProduct(1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.softDeleteProduct(1L);

        assertFalse(product.isActive());
        verify(inventoryService).inventoryContainsProduct(1L);
        verify(productRepository).save(product);
    }

    @Test
    void testDeleteProduct_FailsWhenAlreadyDeleted() {
        Product product = product(1L, null, null, 0, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> productService.softDeleteProduct(1L));

        assertEquals("Product with id 1 is already deleted.", exception.getMessage());
        verify(productRepository).findById(1L);
        verifyNoInteractions(inventoryService);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_FailsWhenInventoryExists() {
        Product product = product(1L, null, null, 0, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.inventoryContainsProduct(1L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> productService.softDeleteProduct(1L));

        assertEquals("Cannot delete product with id 1 because it exists in inventory.", exception.getMessage());
        verify(productRepository).findById(1L);
        verify(inventoryService).inventoryContainsProduct(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_NotFound() {
        when(productRepository.findById(77L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.softDeleteProduct(77L));

        assertEquals("Product not found with id: 77", exception.getMessage());
    }

    @Test
    void testReverseSoftDelete_Success() {
        Product product = product(1L, null, null, 0, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDto result = productService.reverseSoftDelete(1L);

        assertTrue(result.active());
        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void testReverseSoftDelete_ProductNotFound() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.reverseSoftDelete(productId));

        assertEquals("Product not found with id: " + productId, exception.getMessage());
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    private Product product(Long id, String sku, String description, double rrp, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setDescription(description);
        product.setRrp(rrp);
        product.setActive(active);
        return product;
    }

    private ProductUpdateRequestDto updateRequest(String description, Double rrp) {
        return new ProductUpdateRequestDto(description, null, null, null, null, null, null, null, rrp);
    }
}
