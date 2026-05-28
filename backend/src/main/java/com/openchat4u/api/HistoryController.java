package com.openchat4u.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.openchat4u.history.QueryHistory;
import com.openchat4u.history.QueryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final QueryHistoryService historyService;

    @GetMapping("/{tenantCode}")
    public IPage<QueryHistory> listHistory(
            @PathVariable String tenantCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            return historyService.searchByTenantAndKeyword(tenantCode, keyword, page, size);
        }
        return historyService.findByTenant(tenantCode, page, size);
    }

    @GetMapping("/{tenantCode}/{id}")
    public QueryHistory getHistory(@PathVariable String tenantCode, @PathVariable Long id) {
        return historyService.findById(id);
    }

    @DeleteMapping("/{tenantCode}/{id}")
    public Map<String, Boolean> deleteHistory(@PathVariable String tenantCode, @PathVariable Long id) {
        historyService.delete(id);
        return Map.of("success", true);
    }

    @GetMapping("/{tenantCode}/stats")
    public Map<String, Object> getStats(@PathVariable String tenantCode) {
        return Map.of(
            "totalQueries", historyService.countByTenant(tenantCode),
            "successQueries", historyService.countSuccessByTenant(tenantCode)
        );
    }
}
