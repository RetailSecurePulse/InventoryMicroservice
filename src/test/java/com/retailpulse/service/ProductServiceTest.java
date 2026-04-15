package com.retailpulse.service;

import com.retailpulse.dto.request.ProductUpdateRequestDto;
import com.retailpulse.dto.response.*;
import com.retailpulse.entity.Product;
import com.retailpulse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SKUGeneratorService skuGeneratorService;

    @Mock
    private InventoryService inventoryService;  // Add this

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllProducts_Success() {
        // Mock Product object
        Product product = new Product();
        product.setId(1L);
        // When findById is called with 1L, then return the Product object
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponseDto> result = productService.getAllProducts();
        assertFalse(result.isEmpty());
        assertEquals(1L, result.getFirst().id());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void testGetProductById_Success() {
        // Mock Product object
        Product product = new Product();
        product.setId(1L);
        // When findById is called with 1L, then return the Product object
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.getProductById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void testGetProductBySKU_Success() {
        // Mock Product object
        Product product = new Product();
        product.setSku("RP12345");
        // When findBySku is called with RP12345, then return the Product object
        when(productRepository.findBySku("RP12345")).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.getProductBySKU("RP12345");
        assertNotNull(result);
        assertEquals("RP12345", result.sku());
    }

    @Test
    void testSaveProduct_Success() {
        Product product = new Product();
        when(skuGeneratorService.generateSKU()).thenReturn("RP12345");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product savedProduct = invocation.getArgument(0);
            savedProduct.setId(1L); // Simulate the database setting the ID
            return savedProduct;
        });

        ProductResponseDto result = productService.saveProduct(product);
        assertEquals("RP12345", result.sku());
        assertNotNull(result.id()); // Ensure the ID is set
    }

    @Test
    void testUpdateProduct_Success() {
        // Arrange: set up an existing product with valid values
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setSku("12345");
        existingProduct.setDescription("Old Description");
        existingProduct.setRrp(10);
        existingProduct.setActive(true);

        // Arrange: updated product details with a new description and an invalid RRP (-1)
        ProductUpdateRequestDto updatedProduct = new ProductUpdateRequestDto(
                "New Description",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1.0
        );

        // Arrange: stub repository calls
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.<Product>getArgument(0));

        // Act: update the product
        ProductResponseDto result = productService.updateProduct(1L, updatedProduct);

        // Assert: SKU remains unchanged, description is updated,
        // and the original RRP is retained because the new RRP value is invalid.
        assertEquals("12345", result.sku(), "SKU should remain unchanged");
        assertEquals("New Description", result.description(), "Description should be updated");
        assertEquals(10, result.rrp(), "RRP should remain unchanged when updated value is negative");
        assertTrue(result.active(), "Product should remain active");
    }

    @Test
    void testSaveProduct_NegativeRrp() {
        Product product = new Product();
        product.setRrp(-5);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> productService.saveProduct(product));
        assertEquals("Recommended retail price cannot be negative", exception.getMessage());
        verifyNoInteractions(skuGeneratorService);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    public void testUpdateProduct_NotFound() {
        Long productId = 1L;
        ProductUpdateRequestDto updatedProduct = new ProductUpdateRequestDto(
                "New Description",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                20.0
        );

        // Stub repository call to return an empty Optional
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Expect a RuntimeException with the appropriate error message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.updateProduct(productId, updatedProduct));

        assertEquals("Product not found with id: " + productId, exception.getMessage());
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    public void testUpdateProduct_DeletedProduct() {
        Long productId = 1L;
        // Arrange: an existing product that is marked as deleted (inactive)
        Product existingProduct = new Product();
        existingProduct.setId(productId);
        existingProduct.setSku("12345");
        existingProduct.setDescription("Old Description");
        existingProduct.setRrp(10);
        existingProduct.setActive(false);

        // Arrange: details for update (attempt to update a deleted product)
        ProductUpdateRequestDto updatedProduct = new ProductUpdateRequestDto(
                "New Description",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                20.0
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

        // Act & Assert: expect an exception when attempting to update a deleted product
        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.updateProduct(productId, updatedProduct));

        assertEquals("Cannot update a deleted product with id: " + productId, exception.getMessage());
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProductDoesNotChangeIsActive_Success() {
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setSku("12345");
        existingProduct.setDescription("Old Description");
        existingProduct.setActive(true);

        ProductUpdateRequestDto updatedProduct = new ProductUpdateRequestDto(
                "New Description",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.<Product>getArgument(0));

        ProductResponseDto result = productService.updateProduct(1L, updatedProduct);
        assertEquals("12345", result.sku()); // SKU should remain unchanged
        assertEquals("New Description", result.description());
        assertTrue(result.active()); // Ensure the product remains active
    }

    @Test
    void testDeleteProduct_Success() {
        Product product = new Product();
        product.setId(1L);
        product.setActive(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.inventoryContainsProduct(1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.<Product>getArgument(0));

        productService.softDeleteProduct(1L);

        assertFalse(product.isActive()); // Ensure the product is marked as inactive
        verify(inventoryService, times(1)).inventoryContainsProduct(1L);
        verify(productRepository, times(1)).save(product); // Ensure the product is saved
    }

    @Test
    void testDeleteProduct_FailsWhenAlreadyDeleted() {
        Product product = new Product();
        product.setId(1L);
        product.setActive(false);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> productService.softDeleteProduct(1L));

        assertEquals("Product with id 1 is already deleted.", exception.getMessage());
        verify(productRepository, times(1)).findById(1L);
        verifyNoInteractions(inventoryService);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_FailsWhenInventoryExists() {
        Product product = new Product();
        product.setId(1L);
        product.setActive(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.inventoryContainsProduct(1L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> productService.softDeleteProduct(1L));

        assertEquals("Cannot delete product with id 1 because it exists in inventory.", exception.getMessage());
        verify(productRepository, times(1)).findById(1L);
        verify(inventoryService, times(1)).inventoryContainsProduct(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testReverseSoftDelete_Success() {
        Product product = new Product();
        product.setId(1L);
        product.setActive(false); // Initially inactive

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDto result = productService.reverseSoftDelete(1L);

        assertTrue(result.active(), "Product should be re-activated");
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void testReverseSoftDelete_ProductNotFound() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.reverseSoftDelete(productId));

        assertEquals("Product not found with id: " + productId, exception.getMessage());
        verify(productRepository, times(1)).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }
}
