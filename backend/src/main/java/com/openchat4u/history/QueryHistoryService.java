package com.openchat4u.history;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryHistoryService {
    private final QueryHistoryRepository historyRepository;

    public Page<QueryHistory> findByTenant(String tenantCode, Pageable pageable) {
        return historyRepository.findByTenantCodeOrderByCreatedAtDesc(tenantCode, pageable);
    }

    public Page<QueryHistory> searchByTenantAndKeyword(String tenantCode, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByTenant(tenantCode, pageable);
        }
        return historyRepository.findByTenantCodeAndQuestionContainingIgnoreCase(tenantCode, keyword, pageable);
    }

    public QueryHistory findById(Long id) {
        return historyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Query history not found: " + id));
    }

    public QueryHistory save(QueryHistory history) {
        return historyRepository.save(history);
    }

    public void delete(Long id) {
        historyRepository.deleteById(id);
    }

    public long countByTenant(String tenantCode) {
        return historyRepository.countByTenantCode(tenantCode);
    }

    public long countSuccessByTenant(String tenantCode) {
        return historyRepository.countByTenantCodeAndIsTrue(tenantCode, true);
    }
}
