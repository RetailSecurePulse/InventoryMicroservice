package com.retailpulse.config;

import com.retailpulse.dto.InventoryTransactionDetailsDto;
import com.retailpulse.dto.InventoryTransactionProductDto;
import com.retailpulse.dto.response.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Base config: key serializer + TTL, do not cache nulls
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        // Keep explicit per-cache types without enabling default typing.
        ObjectMapper om = JsonMapper.builder().findAndAddModules().build();

        // === Per-type serializers (no default typing) ===
//        // BusinessEntity
//        JacksonJsonRedisSerializer<BusinessEntityResponseDto> beSer = serializer(om, BusinessEntityResponseDto.class);
//        JavaType beListType = om.getTypeFactory().constructCollectionType(List.class, BusinessEntityResponseDto.class);
//        JacksonJsonRedisSerializer<Object> beListSer = serializer(om, beListType);

        // Inventory
        JacksonJsonRedisSerializer<InventoryResponseDto> invSer = serializer(om, InventoryResponseDto.class);
        JavaType invListType = om.getTypeFactory().constructCollectionType(List.class, InventoryResponseDto.class);
        JacksonJsonRedisSerializer<Object> invListSer = serializer(om, invListType);

        // InventoryTransactionProduct (Response)
        JacksonJsonRedisSerializer<InventoryTransactionProductResponseDto> itpRespSer = serializer(om, InventoryTransactionProductResponseDto.class);
        JavaType itpRespListType = om.getTypeFactory().constructCollectionType(List.class, InventoryTransactionProductResponseDto.class);
        JacksonJsonRedisSerializer<Object> itpRespListSer = serializer(om, itpRespListType);

        // InventoryTransaction (Response)
        JacksonJsonRedisSerializer<InventoryTransactionResponseDto> itrSer = serializer(om, InventoryTransactionResponseDto.class);
        JavaType itrListType = om.getTypeFactory().constructCollectionType(List.class, InventoryTransactionResponseDto.class);
        JacksonJsonRedisSerializer<Object> itrListSer = serializer(om, itrListType);

        // Product (Response)
        JacksonJsonRedisSerializer<ProductResponseDto> prodSer = serializer(om, ProductResponseDto.class);
        JavaType prodListType = om.getTypeFactory().constructCollectionType(List.class, ProductResponseDto.class);
        JacksonJsonRedisSerializer<Object> prodListSer = serializer(om, prodListType);

        // InventoryTransactionDetails (DTO)
        JacksonJsonRedisSerializer<InventoryTransactionDetailsDto> itdSer = serializer(om, InventoryTransactionDetailsDto.class);
        JavaType itdListType = om.getTypeFactory().constructCollectionType(List.class, InventoryTransactionDetailsDto.class);
        JacksonJsonRedisSerializer<Object> itdListSer = serializer(om, itdListType);

        // InventoryTransactionProduct (DTO)
        JacksonJsonRedisSerializer<InventoryTransactionProductDto> itpDtoSer = serializer(om, InventoryTransactionProductDto.class);
        JavaType itpDtoListType = om.getTypeFactory().constructCollectionType(List.class, InventoryTransactionProductDto.class);
        JacksonJsonRedisSerializer<Object> itpDtoListSer = serializer(om, itpDtoListType);



        // Per-cache configurations with the correct value serializer
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("inventory", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(invSer))
        );
        cacheConfigs.put("inventoryList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(invListSer))
        );

        cacheConfigs.put("inventoryTransactionProduct", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itpRespSer))
        );
        cacheConfigs.put("inventoryTransactionProductList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itpRespListSer))
        );

        cacheConfigs.put("inventoryTransaction", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itrSer))
        );
        cacheConfigs.put("inventoryTransactionList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itrListSer))
        );

        cacheConfigs.put("product", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(prodSer))
        );
        cacheConfigs.put("productList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(prodListSer))
        );

        cacheConfigs.put("inventoryTransactionDetails", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itdSer))
        );
        cacheConfigs.put("inventoryTransactionDetailsList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itdListSer))
        );

        cacheConfigs.put("inventoryTransactionProductDto", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itpDtoSer))
        );
        cacheConfigs.put("inventoryTransactionProductDtoList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(itpDtoListSer))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base) // default if any other cache is added later
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private static <T> JacksonJsonRedisSerializer<T> serializer(ObjectMapper objectMapper, Class<T> type) {
        return new JacksonJsonRedisSerializer<>(objectMapper, type);
    }

    private static JacksonJsonRedisSerializer<Object> serializer(ObjectMapper objectMapper, JavaType javaType) {
        return new JacksonJsonRedisSerializer<>(objectMapper, javaType);
    }
}
