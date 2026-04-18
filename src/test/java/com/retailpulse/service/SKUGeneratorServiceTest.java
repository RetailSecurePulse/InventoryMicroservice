package com.retailpulse.service;

import com.retailpulse.entity.SKUCounter;
import com.retailpulse.repository.SKUCounterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SKUGeneratorServiceTest {

    @Mock
    private SKUCounterRepository skuCounterRepository;

    @InjectMocks
    private SKUGeneratorService skuGeneratorService;

    @Test
    void generateSKU_usesExistingCounter() {
        when(skuCounterRepository.findByName("product")).thenReturn(Optional.of(new SKUCounter("product", 5L)));
        when(skuCounterRepository.getLastInsertedId()).thenReturn(6L);

        String sku = skuGeneratorService.generateSKU();

        assertEquals("RP6", sku);
        verify(skuCounterRepository).incrementAndStore("product");
    }

    @Test
    void generateSKU_createsCounterWhenMissing() {
        when(skuCounterRepository.findByName("product")).thenReturn(Optional.empty());
        when(skuCounterRepository.save(org.mockito.ArgumentMatchers.any(SKUCounter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(skuCounterRepository.getLastInsertedId()).thenReturn(1L);

        String sku = skuGeneratorService.generateSKU();

        assertEquals("RP1", sku);

        ArgumentCaptor<SKUCounter> captor = ArgumentCaptor.forClass(SKUCounter.class);
        verify(skuCounterRepository).save(captor.capture());
        assertEquals("product", captor.getValue().getName());
        assertEquals(0L, captor.getValue().getCounter());
    }
}
