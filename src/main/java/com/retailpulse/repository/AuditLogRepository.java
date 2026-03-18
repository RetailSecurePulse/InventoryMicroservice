package com.retailpulse.repository;

import com.retailpulse.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    // Useful for Brute Force detection: Count failures for a user in a timeframe
    @Query("SELECT COUNT(a) FROM AuditLogEntity a WHERE a.actor = :username " +
            "AND a.status LIKE 'FAILURE%' AND a.timestamp > :since")
    long countRecentFailures(String username, LocalDateTime since);

    // Useful for Audit Dashboards: Find all actions by a specific admin
    List<AuditLogEntity> findByActorOrderByTimestampDesc(String actor);

    // Useful for Security Reviews: Find all "UPDATE_PERMISSIONS" events
    List<AuditLogEntity> findByAction(String action);
}
