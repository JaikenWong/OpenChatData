package com.openchat4u.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantCode;

    @Column(nullable = false)
    private String action;

    @Column(length = 500)
    private String description;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_ip")
    private String userIp;

    @Column(name = "request_method")
    private String requestMethod;

    @Column(name = "request_path")
    private String requestPath;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
