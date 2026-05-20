package com.openchat4u.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    Page<QueryHistory> findByTenantCodeOrderByCreatedAtDesc(String tenantCode, Pageable pageable);
    Page<QueryHistory> findByTenantCodeAndQuestionContainingIgnoreCase(String tenantCode, String keyword, Pageable pageable);
    long countByTenantCode(String tenantCode);
    long countByTenantCodeAndIsTrue(String tenantCode, boolean success);
}
