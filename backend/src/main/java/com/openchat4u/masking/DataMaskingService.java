package com.openchat4u.masking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataMaskingService {
    private final DataMaskingRuleRepository maskingRuleRepository;

    public List<DataMaskingRule> findByTenant(String tenantCode) {
        return maskingRuleRepository.selectList(
            new LambdaQueryWrapper<DataMaskingRule>()
                .eq(DataMaskingRule::getTenantCode, tenantCode)
                .eq(DataMaskingRule::getIsActive, true)
        );
    }

    public List<DataMaskingRule> findByTenantAndTable(String tenantCode, String tableName) {
        return maskingRuleRepository.selectList(
            new LambdaQueryWrapper<DataMaskingRule>()
                .eq(DataMaskingRule::getTenantCode, tenantCode)
                .eq(DataMaskingRule::getTableName, tableName)
                .eq(DataMaskingRule::getIsActive, true)
        );
    }

    public DataMaskingRule create(DataMaskingRule rule) {
        maskingRuleRepository.insert(rule);
        return rule;
    }

    public void delete(Long id) {
        DataMaskingRule rule = maskingRuleRepository.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("Masking rule not found: " + id);
        }
        rule.setIsActive(false);
        maskingRuleRepository.updateById(rule);
    }

    public List<Map<String, Object>> applyMasking(String tenantCode, String tableName, List<Map<String, Object>> data) {
        List<DataMaskingRule> rules = findByTenantAndTable(tenantCode, tableName);
        if (rules.isEmpty()) {
            return data;
        }

        return data.stream()
            .map(row -> applyRowMasking(row, rules))
            .toList();
    }

    private Map<String, Object> applyRowMasking(Map<String, Object> row, List<DataMaskingRule> rules) {
        for (DataMaskingRule rule : rules) {
            if (row.containsKey(rule.getColumnName())) {
                Object value = row.get(rule.getColumnName());
                if (value != null) {
                    row.put(rule.getColumnName(), maskValue(value.toString(), rule));
                }
            }
        }
        return row;
    }

    private String maskValue(String value, DataMaskingRule rule) {
        return switch (rule.getMaskType()) {
            case "FULL" -> maskFull(value);
            case "PARTIAL" -> maskPartial(value, rule.getMaskPattern());
            case "HASH" -> maskHash(value);
            case "REPLACE" -> rule.getMaskPattern() != null ? rule.getMaskPattern() : "***";
            case "REGEX" -> maskRegex(value, rule.getMaskPattern());
            default -> value;
        };
    }

    private String maskFull(String value) {
        return "*".repeat(Math.max(value.length(), 4));
    }

    private String maskPartial(String value, String pattern) {
        if (value.length() <= 4) return maskFull(value);

        int showStart = 2;
        int showEnd = 2;

        if (pattern != null && pattern.contains("-")) {
            String[] parts = pattern.split("-");
            showStart = Integer.parseInt(parts[0]);
            showEnd = Integer.parseInt(parts[1]);
        }

        String start = value.substring(0, Math.min(showStart, value.length()));
        String end = value.substring(Math.max(showStart, value.length() - showEnd));
        String middle = "*".repeat(Math.max(0, value.length() - showStart - showEnd));

        return start + middle + end;
    }

    private String maskHash(String value) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16) + "...";
        } catch (Exception e) {
            return "***HASH_ERROR***";
        }
    }

    private String maskRegex(String value, String pattern) {
        if (pattern == null || pattern.isEmpty()) return maskFull(value);
        try {
            return value.replaceAll(pattern, "***");
        } catch (Exception e) {
            return maskFull(value);
        }
    }
}
