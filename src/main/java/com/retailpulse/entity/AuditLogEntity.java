package com.retailpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;      // The username or ID of the person acting

    @Column(nullable = false)
    private String action;     // E.g., "USER_LOGIN_SUCCESS", "UPDATE_PERMISSIONS"

    @Column(nullable = false)
    private String status;     // "SUCCESS" or "FAILURE"

    private String details;    // Error messages or extra metadata

    private String ipAddress;  // The origin IP for SIEM "Impossible Travel" alerts

    @Column(nullable = false)
    private LocalDateTime timestamp;

    protected AuditLogEntity() {}

    public AuditLogEntity(String actor, String action, String status, String ipAddress, LocalDateTime timestamp) {
        this.actor = actor;
        this.action = action;
        this.status = status;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

}
