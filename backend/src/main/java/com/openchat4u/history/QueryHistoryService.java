package com.openchat4u.history;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryHistoryService {
    private final QueryHistoryRepository historyRepository;

    public IPage<QueryHistory> findByTenant(String tenantCode, long page, long size) {
        LambdaQueryWrapper<QueryHistory> wrapper = new LambdaQueryWrapper<QueryHistory>()
            .eq(QueryHistory::getTenantCode, tenantCode)
            .orderByDesc(QueryHistory::getCreatedAt);
        return historyRepository.selectPage(new Page<>(page, size), wrapper);
    }

    public IPage<QueryHistory> searchByTenantAndKeyword(String tenantCode, String keyword, long page, long size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByTenant(tenantCode, page, size);
        }
        LambdaQueryWrapper<QueryHistory> wrapper = new LambdaQueryWrapper<QueryHistory>()
            .eq(QueryHistory::getTenantCode, tenantCode)
            .like(QueryHistory::getQuestion, keyword)
            .orderByDesc(QueryHistory::getCreatedAt);
        return historyRepository.selectPage(new Page<>(page, size), wrapper);
    }

    public QueryHistory findById(Long id) {
        QueryHistory h = historyRepository.selectById(id);
        if (h == null) {
            throw new IllegalArgumentException("Query history not found: " + id);
        }
        return h;
    }

    public QueryHistory save(QueryHistory history) {
        historyRepository.insert(history);
        return history;
    }

    public void delete(Long id) {
        historyRepository.deleteById(id);
    }

    public long countByTenant(String tenantCode) {
        return historyRepository.selectCount(
            new LambdaQueryWrapper<QueryHistory>().eq(QueryHistory::getTenantCode, tenantCode)
        );
    }

    public long countSuccessByTenant(String tenantCode) {
        return historyRepository.selectCount(
            new LambdaQueryWrapper<QueryHistory>()
                .eq(QueryHistory::getTenantCode, tenantCode)
                .eq(QueryHistory::getIsSuccess, true)
        );
    }
}
