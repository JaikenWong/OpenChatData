package com.openchat4u.masking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DataMaskingRuleRepository extends JpaRepository<DataMaskingRule, Long> {
    List<DataMaskingRule> findByTenantCodeAndIsActiveTrue(String tenantCode);
    Optional<DataMaskingRule> findByTenantCodeAndTableNameAndColumnNameAndIsActiveTrue(String tenantCode, String tableName, String columnName);
    List<DataMaskingRule> findByTenantCodeAndTableNameAndIsActiveTrue(String tenantCode, String tableName);
}
