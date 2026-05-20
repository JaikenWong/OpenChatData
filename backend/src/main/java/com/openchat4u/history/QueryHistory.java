package com.openchat4u.history;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "query_history")
public class QueryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantCode;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(length = 4000)
    private String sql;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "result_count")
    private Integer resultCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "is_success")
    private Boolean isSuccess;

    @Column(length = 2000)
    private String errorMessage;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
