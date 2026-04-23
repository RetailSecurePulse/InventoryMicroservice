package com.retailpulse.exception;

import com.retailpulse.controller.ErrorResponse;
import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.service.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returnsBadRequest() {
        ResponseEntity<String> response = globalExceptionHandler.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody());
    }

    @Test
    void handleRuntimeException_returnsBadRequest() {
        ResponseEntity<String> response = globalExceptionHandler.handleRuntimeException(new RuntimeException("boom"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("boom", response.getBody());
    }

    @Test
    void applicationExceptionHandler_returnsErrorResponse() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.applicationExceptionHandler(
                new ApplicationException("APP_001", "application error")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("APP_001", response.getBody().getCode());
        assertEquals("application error", response.getBody().getMessage());
    }

    @Test
    void handleBusinessException_returnsErrorResponse() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(
                new BusinessException("BUS_001", "business error")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BUS_001", response.getBody().getCode());
        assertEquals("business error", response.getBody().getMessage());
    }
}
