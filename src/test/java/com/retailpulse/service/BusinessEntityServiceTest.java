package com.retailpulse.service;

import com.retailpulse.client.BusinessEntityClient;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEntityServiceTest {

    @Mock
    private BusinessEntityClient businessEntityClient;

    @InjectMocks
    private BusinessEntityService businessEntityService;

    @Test
    void isValidBusinessEntity_returnsActiveFlag() {
        when(businessEntityClient.getBusinessEntity(1L))
                .thenReturn(new BusinessEntityResponseDto(1L, "Store 1", "Yangon", "STORE", false, true));

        assertTrue(businessEntityService.isValidBusinessEntity(1L));
    }

    @Test
    void isValidBusinessEntity_returnsFalseForInactiveEntity() {
        when(businessEntityClient.getBusinessEntity(2L))
                .thenReturn(new BusinessEntityResponseDto(2L, "Store 2", "Mandalay", "STORE", false, false));

        assertFalse(businessEntityService.isValidBusinessEntity(2L));
    }

    @Test
    void isValidBusinessEntity_returnsTrueWhenClientFails() {
        when(businessEntityClient.getBusinessEntity(3L)).thenThrow(new RuntimeException("downstream error"));

        assertTrue(businessEntityService.isValidBusinessEntity(3L));
    }

    @Test
    void isValidBusinessEntity_returnsTrueWhenClientReturnsNull() {
        when(businessEntityClient.getBusinessEntity(4L)).thenReturn(null);

        assertTrue(businessEntityService.isValidBusinessEntity(4L));
    }

    @Test
    void isExternalBusinessEntity_throwsForNullId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> businessEntityService.isExternalBusinessEntity(null));

        assertEquals("businessEntityId cannot be null", exception.getMessage());
    }

    @Test
    void isExternalBusinessEntity_returnsExternalFlag() {
        when(businessEntityClient.getBusinessEntity(5L))
                .thenReturn(new BusinessEntityResponseDto(5L, "Vendor", "Singapore", "VENDOR", true, true));

        assertTrue(businessEntityService.isExternalBusinessEntity(5L));
    }

    @Test
    void isExternalBusinessEntity_wrapsNullResponse() {
        when(businessEntityClient.getBusinessEntity(6L)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> businessEntityService.isExternalBusinessEntity(6L));

        assertEquals("Unable to fetch business entity with id: 6", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void isExternalBusinessEntity_wrapsClientFailure() {
        when(businessEntityClient.getBusinessEntity(7L)).thenThrow(new RuntimeException("network"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> businessEntityService.isExternalBusinessEntity(7L));

        assertEquals("Unable to fetch business entity with id: 7", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void allBusinessEntityResponseDetails_returnsList() {
        List<BusinessEntityResponseDto> response = List.of(
                new BusinessEntityResponseDto(1L, "Store 1", "Yangon", "STORE", false, true),
                new BusinessEntityResponseDto(2L, "Warehouse", "Bago", "WAREHOUSE", false, true)
        );
        when(businessEntityClient.getAllBusinessEntity()).thenReturn(response);

        assertEquals(response, businessEntityService.allBusinessEntityResponseDetails());
    }

    @Test
    void allBusinessEntityResponseDetails_wrapsNullResponse() {
        when(businessEntityClient.getAllBusinessEntity()).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> businessEntityService.allBusinessEntityResponseDetails());

        assertTrue(exception.getMessage().contains("Unable to fetch business entity"));
    }

    @Test
    void allBusinessEntityResponseDetails_wrapsClientFailure() {
        when(businessEntityClient.getAllBusinessEntity()).thenThrow(new RuntimeException("timeout"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> businessEntityService.allBusinessEntityResponseDetails());

        assertTrue(exception.getMessage().contains("Unable to fetch business entity"));
    }
}
