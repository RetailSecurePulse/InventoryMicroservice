package com.retailpulse.controller;

import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.dto.response.InventoryResponseDto;
import com.retailpulse.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger logger = Logger.getLogger(InventoryController.class.getName());
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDto>> getAllInventories() {
        logger.info("Fetching all inventories");
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponseDto> getInventoryById(@PathVariable Long id) {
        logger.info("Fetching inventory with id: " + id);
        InventoryResponseDto response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/productId/{id}")
    public ResponseEntity<List<InventoryResponseDto>> getInventoryByProductId(@PathVariable Long id) {
        logger.info("Fetching inventory with productId: " + id);
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(id));
    }

    @GetMapping("/businessEntityId/{businessEntityId}")
    public ResponseEntity<List<InventoryResponseDto>> getInventoryByBusinessEntityId(@PathVariable Long businessEntityId) {
        logger.info("Fetching inventory with businessEntityId: " + businessEntityId);
        return ResponseEntity.ok(inventoryService.getInventoryByBusinessEntityId(businessEntityId));
    }

    @GetMapping("/productId/{productId}/businessEntityId/{businessEntityId}")
    public ResponseEntity<InventoryResponseDto> getInventoryByProductIdAndBusinessEntityId(@PathVariable Long productId, @PathVariable Long businessEntityId) {
        logger.info("Fetching inventory with businessEntityId (" + businessEntityId + ") and productId (" + productId + ")");
        InventoryResponseDto response = inventoryService.getInventoryByProductIdAndBusinessEntityId(productId, businessEntityId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salesUpdate")
    public ResponseEntity<Void> salesUpdateStocks(@Valid @RequestBody InventoryUpdateRequestDto request)
    {
        logger.info("Deducting inventories with businessEntityId (" + request.businessEntityId() + ")");
        inventoryService.salesUpdateStocks(request);
        
        return ResponseEntity.ok().build();
    }    
}