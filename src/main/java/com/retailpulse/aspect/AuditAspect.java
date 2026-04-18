package com.retailpulse.aspect;

import com.retailpulse.annotation.AuditLog;
import com.retailpulse.entity.AuditLogEntity;
import com.retailpulse.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String remoteAddress = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getRemoteAddr();

        try {
            Object result = joinPoint.proceed(); // Execute the actual service method
            saveLog(actor, auditLog.action(), "SUCCESS", remoteAddress);
            return result;
        } catch (Exception e) {
            saveLog(actor, auditLog.action(), "FAILURE: " + e.getMessage(), remoteAddress);
            throw e;
        }
    }

    private void saveLog(String actor, String action, String status, String ip) {
        AuditLogEntity records = new AuditLogEntity(actor, action, status, ip, LocalDateTime.now());
        auditLogRepository.save(records);
    }

}
