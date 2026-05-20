package com.openchat4u.masking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataMaskingServiceTest {

    @Mock
    private DataMaskingRuleRepository maskingRuleRepository;

    private DataMaskingService maskingService;

    @BeforeEach
    void setUp() {
        maskingService = new DataMaskingService(maskingRuleRepository);
    }

    @Test
    void testFindByTenant() {
        String tenantCode = "tenant1";
        List<DataMaskingRule> expected = Arrays.asList(
            createRule(1L, tenantCode, "users", "phone", "PARTIAL", "2-2"),
            createRule(2L, tenantCode, "users", "email", "HASH", null)
        );
        
        when(maskingRuleRepository.findByTenantCodeAndIsActiveTrue(tenantCode))
            .thenReturn(expected);

        List<DataMaskingRule> result = maskingService.findByTenant(tenantCode);

        assertEquals(2, result.size());
        verify(maskingRuleRepository).findByTenantCodeAndIsActiveTrue(tenantCode);
    }

    @Test
    void testFindByTenantAndTable() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<DataMaskingRule> expected = List.of(
            createRule(1L, tenantCode, tableName, "phone", "PARTIAL", "2-2")
        );
        
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(expected);

        List<DataMaskingRule> result = maskingService.findByTenantAndTable(tenantCode, tableName);

        assertEquals(1, result.size());
    }

    @Test
    void testCreate() {
        DataMaskingRule newRule = createRule(null, "tenant1", "users", "phone", "FULL", null);
        when(maskingRuleRepository.save(any(DataMaskingRule.class))).thenAnswer(invocation -> {
            DataMaskingRule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        DataMaskingRule result = maskingService.create(newRule);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testDelete() {
        DataMaskingRule existing = createRule(1L, "tenant1", "users", "phone", "FULL", null);
        when(maskingRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(maskingRuleRepository.save(any(DataMaskingRule.class))).thenReturn(existing);

        maskingService.delete(1L);

        assertFalse(existing.getIsActive());
        verify(maskingRuleRepository).save(existing);
    }

    @Test
    void testDeleteNotFound() {
        when(maskingRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> maskingService.delete(999L));
    }

    @Test
    void testApplyMaskingNoRules() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("张三", "13812345678", "zhangsan@example.com"),
            createRow("李四", "13987654321", "lisi@example.com")
        );
        
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of());

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals(data, result);
    }

    @Test
    void testApplyMaskingFull() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "FULL", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("***********", result.get(0).get("phone"));
        assertEquals("张三", result.get(0).get("name"));
    }

    @Test
    void testApplyMaskingPartial() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "PARTIAL", "3-4");
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("138****5678", result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingPartialDefaultPattern() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "PARTIAL", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("13*******78", result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingPartialShortValue() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "1234", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "PARTIAL", "2-2");
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("****", result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingReplace() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "REPLACE", "***");
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("***", result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingReplaceDefault() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "REPLACE", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("***", result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingHash() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "HASH", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        String hashed = (String) result.get(0).get("phone");
        assertNotNull(hashed);
        assertTrue(hashed.endsWith("..."));
        assertEquals(19, hashed.length());
    }

    @Test
    void testApplyMaskingRegex() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "REGEX", "\\d{4}");
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("******678", result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingMultipleRules() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule phoneRule = createRule(1L, tenantCode, tableName, "phone", "PARTIAL", "3-4");
        DataMaskingRule emailRule = createRule(2L, tenantCode, tableName, "email", "FULL", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(Arrays.asList(phoneRule, emailRule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("138****5678", result.get(0).get("phone"));
        assertEquals("********************", result.get(0).get("email"));
    }

    @Test
    void testApplyMaskingNullValue() {
        String tenantCode = "tenant1";
        String tableName = "users";
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "张三");
        row.put("phone", null);
        List<Map<String, Object>> data = List.of(row);
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "phone", "FULL", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertNull(result.get(0).get("phone"));
    }

    @Test
    void testApplyMaskingNonExistentColumn() {
        String tenantCode = "tenant1";
        String tableName = "users";
        List<Map<String, Object>> data = List.of(
            createRow("张三", "13812345678", "zhangsan@example.com")
        );
        
        DataMaskingRule rule = createRule(1L, tenantCode, tableName, "nonexistent", "FULL", null);
        when(maskingRuleRepository.findByTenantCodeAndTableNameAndIsActiveTrue(tenantCode, tableName))
            .thenReturn(List.of(rule));

        List<Map<String, Object>> result = maskingService.applyMasking(tenantCode, tableName, data);

        assertEquals("13812345678", result.get(0).get("phone"));
    }

    private DataMaskingRule createRule(Long id, String tenantCode, String tableName, String columnName, String maskType, String maskPattern) {
        DataMaskingRule rule = new DataMaskingRule();
        rule.setId(id);
        rule.setTenantCode(tenantCode);
        rule.setTableName(tableName);
        rule.setColumnName(columnName);
        rule.setMaskType(maskType);
        rule.setMaskPattern(maskPattern);
        rule.setIsActive(true);
        return rule;
    }

    private Map<String, Object> createRow(String name, String phone, String email) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("phone", phone);
        row.put("email", email);
        return row;
    }
}
