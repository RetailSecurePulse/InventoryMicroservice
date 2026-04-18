package com.retailpulse.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void cacheManager_registersExpectedCaches() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        @SuppressWarnings("unchecked")
        Map<String, RedisCacheConfiguration> cacheConfigurations =
                (Map<String, RedisCacheConfiguration>) ReflectionTestUtils.getField(cacheManager, "initialCacheConfiguration");

        assertNotNull(cacheManager);
        assertNotNull(cacheConfigurations);
        assertEquals(
                Set.of(
                        "inventory",
                        "inventoryList",
                        "inventoryTransactionProduct",
                        "inventoryTransactionProductList",
                        "inventoryTransaction",
                        "inventoryTransactionList",
                        "product",
                        "productList",
                        "inventoryTransactionDetails",
                        "inventoryTransactionDetailsList",
                        "inventoryTransactionProductDto",
                        "inventoryTransactionProductDtoList"
                ),
                cacheConfigurations.keySet()
        );
        assertTrue(cacheManager.getCache("inventory") != null);
        assertTrue(cacheManager.getCache("productList") != null);
    }
}
