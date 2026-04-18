package com.retailpulse.service;

import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.dto.response.InventoryResponseDto;
import com.retailpulse.entity.Inventory;
import com.retailpulse.repository.InventoryRepository;
import com.retailpulse.service.exception.BusinessException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Service
public class InventoryService {
    private static final Logger logger = Logger.getLogger(InventoryService.class.getName());

    private static final String INVENTORY_NOT_FOUND = "INVENTORY_NOT_FOUND";
    private static final String INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND = "INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND";
    private static final String INVENTORY_NOT_FOUND_DESC = "Inventory not found with id: ";
    private static final String INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND_DESC = "Inventory by Product and Business Entity not found (ProductId, Business Entity): ";

    private static final String INVALID_BUSINESS_ENTITY = "INVALID_BUSINESS_ENTITY";
    private static final String INVALID_BUSINESS_ENTITY_DESC = "Not a valid business entity: ";

    private final InventoryRepository inventoryRepository;
    private final BusinessEntityService businessEntityService;

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, BusinessEntityService businessEntityService) {
        this.inventoryRepository = inventoryRepository;
        this.businessEntityService = businessEntityService;
    }

    @Cacheable(value = "inventoryList", key = "'all'", sync = true)
    public List<InventoryResponseDto> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(inventoryEntity -> new InventoryResponseDto(
                        inventoryEntity.getId(),
                        inventoryEntity.getProductId(),
                        inventoryEntity.getBusinessEntityId(),
                        inventoryEntity.getQuantity(),
                        inventoryEntity.getTotalCostPrice()
                ))
                .toList();
    }

    @Cacheable(value = "inventory", key = "#id", sync = true)
    public InventoryResponseDto getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(INVENTORY_NOT_FOUND, INVENTORY_NOT_FOUND_DESC + id));

        return new InventoryResponseDto(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getBusinessEntityId(),
                inventory.getQuantity(),
                inventory.getTotalCostPrice()
        );
    }

    @Cacheable(value = "inventoryList", key = "'byProduct:' + #productId", sync = true)
    public List<InventoryResponseDto> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(inventoryEntity -> new InventoryResponseDto(
                        inventoryEntity.getId(),
                        inventoryEntity.getProductId(),
                        inventoryEntity.getBusinessEntityId(),
                        inventoryEntity.getQuantity(),
                        inventoryEntity.getTotalCostPrice()
                ))
                .toList();
    }

    @Cacheable(value = "inventoryList", key = "'byBE:' + #businessEntityId", sync = true)
    public List<InventoryResponseDto> getInventoryByBusinessEntityId(Long businessEntityId) {
        if (!businessEntityService.isValidBusinessEntity(businessEntityId)) {
            throw new BusinessException(INVALID_BUSINESS_ENTITY, INVALID_BUSINESS_ENTITY_DESC + businessEntityId);
        }

        return inventoryRepository.findByBusinessEntityId(businessEntityId).stream()
                .map(inventoryEntity -> new InventoryResponseDto(
                        inventoryEntity.getId(),
                        inventoryEntity.getProductId(),
                        inventoryEntity.getBusinessEntityId(),
                        inventoryEntity.getQuantity(),
                        inventoryEntity.getTotalCostPrice()
                ))
                .toList();

    }

    @Cacheable(value = "inventory", key = "'byProductAndBE:' + #productId + ':' + #businessEntityId", sync = true)
     public InventoryResponseDto getInventoryByProductIdAndBusinessEntityId(Long productId, Long businessEntityId) {
        if (!businessEntityService.isValidBusinessEntity(businessEntityId)) {
            throw new BusinessException(INVALID_BUSINESS_ENTITY, INVALID_BUSINESS_ENTITY_DESC + businessEntityId);
        }

        Inventory inventory = inventoryRepository.findByProductIdAndBusinessEntityId(productId, businessEntityId)
                .orElseThrow(() -> new BusinessException(INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND,
                        INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND_DESC + "(" + productId + ", " + businessEntityId + ")"));

        return new InventoryResponseDto(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getBusinessEntityId(),
                inventory.getQuantity(),
                inventory.getTotalCostPrice()
        );
    }

    public boolean inventoryContainsProduct(Long productId) {
        List<InventoryResponseDto> inventoryList = getInventoryByProductId(productId);
        return !inventoryList.isEmpty();
    }

    // Not exposed in controller - Inventory should only be changed by Inventory Summary
    @CacheEvict(value = {"inventory", "inventoryList"}, allEntries = true)
    public Inventory saveInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    // Not exposed in controller - Inventory should only be changed by Inventory Summary
    @CacheEvict(value = {"inventory", "inventoryList"}, allEntries = true)
    public Inventory updateInventory(Long id, @NotNull Inventory inventoryDetails) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(INVENTORY_NOT_FOUND, INVENTORY_NOT_FOUND_DESC + id));

        // Update fields from the incoming details if provided
        updateField(inventoryDetails.getProductId(), inventory::setProductId);
        updateField(inventoryDetails.getBusinessEntityId(), inventory::setBusinessEntityId);

        if (inventoryDetails.getQuantity() >= 0) {
            updateField(inventoryDetails.getQuantity(), inventory::setQuantity);
        }

        if (inventoryDetails.getTotalCostPrice() >= 0) {
            updateField(inventoryDetails.getTotalCostPrice(), inventory::setTotalCostPrice);
        }
        return inventoryRepository.save(inventory);
    }

    // Generic helper method for updating fields
    private <T> void updateField(T newValue, Consumer<T> updater) {
        if (newValue == null) {
            return;
        }
        if (newValue instanceof String && ((String) newValue).isEmpty()) {
            return;
        }
        updater.accept(newValue);
    }

    // Not exposed in controller - Inventory should only be changed by Inventory Summary
    @CacheEvict(value = {"inventory", "inventoryList"}, allEntries = true)
    public Inventory deleteInventory(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(INVENTORY_NOT_FOUND, INVENTORY_NOT_FOUND_DESC + id));

        inventoryRepository.delete(inventory);
        return inventory;
    }

    @CacheEvict(value = {"inventory", "inventoryList"}, allEntries = true)
    @Transactional
    public void salesUpdateStocks(@NotNull InventoryUpdateRequestDto request) {
        Long businessEntityId = request.businessEntityId();
        logger.info("Starting salesUpdateStocks for businessEntityId: " + businessEntityId);

        if (!businessEntityService.isValidBusinessEntity(businessEntityId)) {
          logger.warning("Invalid business entity: " + businessEntityId);
          throw new BusinessException(INVALID_BUSINESS_ENTITY, INVALID_BUSINESS_ENTITY_DESC + businessEntityId);
        }

        List<InventoryUpdateRequestDto.InventoryItem> failedItems = new java.util.ArrayList<>();
        for (InventoryUpdateRequestDto.InventoryItem item : request.items()) {
          Long productId = item.productId();
          int quantityToDeduct = item.quantity();
          logger.info("Processing productId: " + productId + ", quantityToDeduct: " + quantityToDeduct);

          Inventory inventory = inventoryRepository.findByProductIdAndBusinessEntityId(productId, businessEntityId)
            .orElseThrow(() -> {
              logger.warning("Inventory not found for productId: " + productId + ", businessEntityId: " + businessEntityId);
              return new BusinessException(
                INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND,
                INVENTORY_BY_PRODUCT_AND_BUSINESS_ENTITY_NOT_FOUND_DESC + "(" + productId + ", " + businessEntityId + ")"
              );
            });

          int currentQuantity = inventory.getQuantity();
          
          if (currentQuantity < quantityToDeduct) {
            logger.warning("Insufficient stock for productId: " + productId + ". Available: " + currentQuantity + ", Requested: " + quantityToDeduct);
            failedItems.add(item);
          } else {
            int newQuantity = currentQuantity - quantityToDeduct;
            inventory.setQuantity(newQuantity);
            logger.info("Updated inventory quantity for productId: " + productId + " to " + newQuantity);
            inventoryRepository.save(inventory);
          }
        }

        if (!failedItems.isEmpty()) {
          String failedProducts = failedItems.stream()
            .map(i -> String.valueOf(i.productId()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
          logger.warning("Throwing BusinessException for insufficient stock on products: " + failedProducts);
          throw new BusinessException("INSUFFICIENT_STOCK", "Insufficient stock for products: " + failedProducts);
        }
      logger.info("salesUpdateStocks completed successfully for businessEntityId: " + businessEntityId);
    }
}