package com.retailpulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailpulse.dto.request.ProductCreateRequestDto;
import com.retailpulse.dto.request.ProductUpdateRequestDto;
import com.retailpulse.dto.response.ProductResponseDto;
import com.retailpulse.entity.Product;
import com.retailpulse.exception.GlobalExceptionHandler;
import com.retailpulse.service.ProductService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCreateProduct_usesRequestDtoAndMapsToEntity() throws Exception {
        ProductCreateRequestDto request = createRequest();
        ProductResponseDto response = productResponse("Denim Jacket", "Outerwear", "Jackets", "Levis", "USA", "each", "LEV", "1234567890", 79.99, false);
        when(productService.saveProduct(any(Product.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("RP12345"))
                .andExpect(jsonPath("$.description").value("Denim Jacket"))
                .andExpect(jsonPath("$.rrp").value(79.99))
                .andExpect(jsonPath("$.active").value(false));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).saveProduct(captor.capture());

        Product mapped = captor.getValue();
        assertEquals("Denim Jacket", mapped.getDescription());
        assertEquals("Outerwear", mapped.getCategory());
        assertEquals("Jackets", mapped.getSubcategory());
        assertEquals("Levis", mapped.getBrand());
        assertEquals("USA", mapped.getOrigin());
        assertEquals("each", mapped.getUom());
        assertEquals("LEV", mapped.getVendorCode());
        assertEquals("1234567890", mapped.getBarcode());
        assertEquals(79.99, mapped.getRrp());
        assertFalse(mapped.isActive());
    }

    @Test
    void testUpdateProduct_usesUpdateDto() throws Exception {
        ProductUpdateRequestDto request = updateRequest();
        ProductResponseDto response = productResponse("Updated Jacket", "Outerwear", "Bomber", "Levis", "Italy", "each", "VENDOR-2", "1234567890", 99.5, true);
        when(productService.updateProduct(eq(1L), any(ProductUpdateRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Updated Jacket"))
                .andExpect(jsonPath("$.subcategory").value("Bomber"))
                .andExpect(jsonPath("$.origin").value("Italy"))
                .andExpect(jsonPath("$.vendorCode").value("VENDOR-2"))
                .andExpect(jsonPath("$.rrp").value(99.5));

        ArgumentCaptor<ProductUpdateRequestDto> captor = ArgumentCaptor.forClass(ProductUpdateRequestDto.class);
        verify(productService).updateProduct(eq(1L), captor.capture());

        ProductUpdateRequestDto mapped = captor.getValue();
        assertEquals("Updated Jacket", mapped.description());
        assertNull(mapped.category());
        assertEquals("Bomber", mapped.subcategory());
        assertNull(mapped.brand());
        assertEquals("Italy", mapped.origin());
        assertNull(mapped.uom());
        assertEquals("VENDOR-2", mapped.vendorCode());
        assertNull(mapped.barcode());
        assertEquals(99.5, mapped.rrp());
    }

    private ProductCreateRequestDto createRequest() {
        return new ProductCreateRequestDto(
                "Denim Jacket", "Outerwear", "Jackets", "Levis", "USA",
                "each", "LEV", "1234567890", 79.99, false
        );
    }

    private ProductUpdateRequestDto updateRequest() {
        return new ProductUpdateRequestDto(
                "Updated Jacket", null, "Bomber", null, "Italy", null, "VENDOR-2", null, 99.5
        );
    }

    private ProductResponseDto productResponse(String description, String category, String subcategory, String brand,
                                               String origin, String uom, String vendorCode, String barcode,
                                               double rrp, boolean active) {
        return new ProductResponseDto(1L, "RP12345", description, category, subcategory, brand, origin, uom, vendorCode, barcode, rrp, active);
    }

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
