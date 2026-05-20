package com.openchat4u.masking;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_masking_rules")
public class DataMaskingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantCode;

    @Column(nullable = false, length = 100)
    private String tableName;

    @Column(nullable = false, length = 100)
    private String columnName;

    @Column(nullable = false)
    private String maskType;

    @Column(length = 200)
    private String maskPattern;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
