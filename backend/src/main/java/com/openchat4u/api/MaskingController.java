package com.openchat4u.api;

import com.openchat4u.masking.DataMaskingRule;
import com.openchat4u.masking.DataMaskingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masking")
@RequiredArgsConstructor
public class MaskingController {

    private final DataMaskingService maskingService;

    @GetMapping("/{tenantCode}")
    public List<DataMaskingRule> listRules(@PathVariable String tenantCode) {
        return maskingService.findByTenant(tenantCode);
    }

    @GetMapping("/{tenantCode}/{tableName}")
    public List<DataMaskingRule> listRulesByTable(@PathVariable String tenantCode, @PathVariable String tableName) {
        return maskingService.findByTenantAndTable(tenantCode, tableName);
    }

    @PostMapping
    public DataMaskingRule createRule(@RequestBody DataMaskingRule rule) {
        return maskingService.create(rule);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> deleteRule(@PathVariable Long id) {
        maskingService.delete(id);
        return Map.of("success", true);
    }

    @GetMapping("/types")
    public List<Map<String, String>> getMaskTypes() {
        return List.of(
            Map.of("value", "FULL", "label", "Full Mask", "description", "Replace entire value with *"),
            Map.of("value", "PARTIAL", "label", "Partial Mask", "description", "Show first/last N characters (pattern: 2-2)"),
            Map.of("value", "HASH", "label", "Hash", "description", "SHA-256 hash of value"),
            Map.of("value", "REPLACE", "label", "Replace", "description", "Replace with fixed string"),
            Map.of("value", "REGEX", "label", "Regex", "description", "Replace matching pattern")
        );
    }
}
