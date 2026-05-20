package com.openchat4u.api;

import com.openchat4u.history.QueryHistory;
import com.openchat4u.history.QueryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final QueryHistoryService historyService;

    @GetMapping("/{tenantCode}")
    public Page<QueryHistory> listHistory(
            @PathVariable String tenantCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return historyService.searchByTenantAndKeyword(tenantCode, keyword, pageRequest);
        }
        return historyService.findByTenant(tenantCode, pageRequest);
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
