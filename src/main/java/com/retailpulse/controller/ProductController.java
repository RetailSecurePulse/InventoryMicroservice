package com.retailpulse.controller;

import com.retailpulse.dto.request.ProductCreateRequestDto;
import com.retailpulse.dto.request.ProductUpdateRequestDto;
import com.retailpulse.dto.response.ProductResponseDto;
import com.retailpulse.entity.Product;
import com.retailpulse.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = Logger.getLogger(ProductController.class.getName());
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        logger.info("Fetching all products");
        List<ProductResponseDto> product = productService.getAllProducts();
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        logger.info("Fetching product with id: " + id);
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponseDto> getProductBySKU(@PathVariable String sku) {
        if (sku == null || sku.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU cannot be null or empty");
        }

        String strSku = sku.replaceAll("[\n\r]", "_");
        logger.info("Fetching product with sku: " + strSku);
        ProductResponseDto product = productService.getProductBySKU(strSku);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductCreateRequestDto productRequest) {
        logger.info("Received request to create product: " + productRequest);
        try {
            Product product = toProduct(productRequest);
            ProductResponseDto createdProduct = productService.saveProduct(product);
            logger.info("Successfully created product with sku: " + createdProduct.sku());
            return ResponseEntity.ok(createdProduct);
        } catch (Exception e) {
            logger.severe("Error creating product: " + e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequestDto productRequest) {
        logger.info("Received request to update product with id: " + id);
        try {
            ProductResponseDto updatedProduct = productService.updateProduct(id, productRequest);
            logger.info("Successfully updated product with id: " + updatedProduct.id());
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            logger.severe("Error updating product: " + e.getMessage());
            throw e;
        }
    }

    private Product toProduct(ProductCreateRequestDto request) {
        Product product = new Product();
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setSubcategory(request.subcategory());
        product.setBrand(request.brand());
        product.setOrigin(request.origin());
        product.setUom(request.uom());
        product.setVendorCode(request.vendorCode());
        product.setBarcode(request.barcode());
        if (request.rrp() != null) {
            product.setRrp(request.rrp());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        return product;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("Deleting product with id: " + id);
        productService.softDeleteProduct(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reverseSoftDelete/{id}")
    public ResponseEntity<ProductResponseDto> reverseSoftDeleteProduct(@PathVariable Long id) {
        logger.info("Reverse soft delete of product with id: " + id);
        ProductResponseDto product = productService.reverseSoftDelete(id);
        return ResponseEntity.ok(product);
    }
}
