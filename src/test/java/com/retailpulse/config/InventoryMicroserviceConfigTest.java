package com.retailpulse.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMicroserviceConfigTest {

    @Test
    void corsConfigurationSource_usesConfiguredOriginAndHeaders() {
        InventoryMicroserviceConfig config = new InventoryMicroserviceConfig();
        ReflectionTestUtils.setField(config, "originURL", "https://retailpulse.example");

        CorsConfigurationSource source = ReflectionTestUtils.invokeMethod(config, "corsConfigurationSource");
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/products"));

        assertEquals(List.of("https://retailpulse.example"), configuration.getAllowedOriginPatterns());
        assertEquals(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"), configuration.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type"), configuration.getAllowedHeaders());
        assertEquals(List.of("Authorization"), configuration.getExposedHeaders());
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }

    @Test
    void jwtAuthenticationConverter_mapsRolesClaimToSpringAuthorities() {
        InventoryMicroserviceConfig config = new InventoryMicroserviceConfig();

        JwtAuthenticationConverter converter = ReflectionTestUtils.invokeMethod(config, "jwtAuthenticationConverter");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("manager")
                .claim("roles", List.of("ADMIN", "MANAGER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_MANAGER"));
    }
}
