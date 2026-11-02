package com.gameshowcenter.backend.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hardened Global Exception Handler conforming to SOC 2 CC7.1 and ISO 27001:2022 A.8.12/A.8.26.
 * Prevents information disclosure (leakage of stack traces and internal paths) while maintaining
 * traceable server-side audit logs with unique incident correlation IDs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Catches business validation exceptions (HTTP 400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(IllegalArgumentException ex) {
        log.warn("Business validation exception: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Validation failed");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Catches any unexpected server error (HTTP 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralExceptions(Exception ex) {
        String incidentId = UUID.randomUUID().toString();
        log.error("Internal server error [Incident ID: {}]: ", incidentId, ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal server error");
        errorResponse.put("incidentId", incidentId);
        errorResponse.put("message", "An unexpected internal error occurred. Please contact administrator referencing the incident ID.");
        errorResponse.put("mensaje", "Ocurrió un error interno no esperado. Contacte al administrador indicando el ID de incidente.");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}