package com.retailpulse.aspect;

import com.retailpulse.annotation.AuditLog;
import com.retailpulse.entity.AuditLogEntity;
import com.retailpulse.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private AuditLog auditLog;

    @InjectMocks
    private AuditAspect auditAspect;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void audit_savesSuccessLog() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("william", "password"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(auditLog.action()).thenReturn("CREATE_PRODUCT");
        when(proceedingJoinPoint.proceed()).thenReturn("done");

        Object result = auditAspect.audit(proceedingJoinPoint, auditLog);

        assertEquals("done", result);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("william", captor.getValue().getActor());
        assertEquals("CREATE_PRODUCT", captor.getValue().getAction());
        assertEquals("SUCCESS", captor.getValue().getStatus());
        assertEquals("10.0.0.1", captor.getValue().getIpAddress());
    }

    @Test
    void audit_savesFailureLogAndRethrows() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("cashier", "password"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(auditLog.action()).thenReturn("UPDATE_PRODUCT");
        when(proceedingJoinPoint.proceed()).thenThrow(new IllegalStateException("db unavailable"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> auditAspect.audit(proceedingJoinPoint, auditLog));

        assertEquals("db unavailable", exception.getMessage());

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("cashier", captor.getValue().getActor());
        assertEquals("UPDATE_PRODUCT", captor.getValue().getAction());
        assertEquals("FAILURE: db unavailable", captor.getValue().getStatus());
        assertEquals("10.0.0.2", captor.getValue().getIpAddress());
    }
}
