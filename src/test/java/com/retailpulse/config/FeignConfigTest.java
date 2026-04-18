package com.retailpulse.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignConfigTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void feignLoggerLevel_returnsFull() {
        FeignConfig feignConfig = new FeignConfig(tracer);

        assertEquals(Logger.Level.FULL, feignConfig.feignLoggerLevel());
    }

    @Test
    void interceptor_addsTraceHeadersAndJwtBearerToken() {
        FeignConfig feignConfig = new FeignConfig(tracer);
        RequestInterceptor interceptor = feignConfig.oauth2BearerForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        template.method("GET");
        template.uri("/api/products");

        Jwt jwt = Jwt.withTokenValue("jwt-token-value")
                .header("alg", "none")
                .subject("user")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");

        interceptor.apply(template);

        assertEquals(List.of("trace-123"), template.headers().get("X-B3-TraceId").stream().toList());
        assertEquals(List.of("span-456"), template.headers().get("X-B3-SpanId").stream().toList());
        assertEquals(List.of("Bearer jwt-token-value"), template.headers().get(HttpHeaders.AUTHORIZATION).stream().toList());
    }

    @Test
    void interceptor_readsBearerTokenFromBearerAuthentication() {
        FeignConfig feignConfig = new FeignConfig(tracer);
        RequestInterceptor interceptor = feignConfig.oauth2BearerForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        template.method("POST");
        template.uri("/api/orders");

        DefaultOAuth2AuthenticatedPrincipal principal =
                new DefaultOAuth2AuthenticatedPrincipal(Map.of("sub", "cashier"), List.of());
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        SecurityContextHolder.getContext().setAuthentication(
                new BearerTokenAuthentication(principal, accessToken, principal.getAuthorities())
        );

        when(tracer.currentSpan()).thenReturn(null);

        interceptor.apply(template);

        assertEquals(List.of("Bearer access-token"), template.headers().get(HttpHeaders.AUTHORIZATION).stream().toList());
        assertFalse(template.headers().containsKey("X-B3-TraceId"));
    }

    @Test
    void interceptor_fallsBackToRequestHeaderWhenSecurityContextHasNoBearerToken() {
        FeignConfig feignConfig = new FeignConfig(tracer);
        RequestInterceptor interceptor = feignConfig.oauth2BearerForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        template.method("GET");
        template.uri("/api/inventory");

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pw"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(tracer.currentSpan()).thenReturn(null);

        interceptor.apply(template);

        assertEquals(List.of("Bearer header-token"), template.headers().get(HttpHeaders.AUTHORIZATION).stream().toList());
    }

    @Test
    void interceptor_skipsAuthorizationWhenNoTokenExists() {
        FeignConfig feignConfig = new FeignConfig(tracer);
        RequestInterceptor interceptor = feignConfig.oauth2BearerForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        template.method("GET");
        template.uri("/api/inventory");

        when(tracer.currentSpan()).thenReturn(null);

        interceptor.apply(template);

        assertTrue(template.headers().getOrDefault(HttpHeaders.AUTHORIZATION, List.of()).isEmpty());
    }
}
