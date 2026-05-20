package com.openchat4u.dictionary;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DictionaryRepository extends JpaRepository<Dictionary, Long> {
    List<Dictionary> findByTenantCodeAndTypeAndIsActiveTrue(String tenantCode, String type);
    List<Dictionary> findByTenantCodeAndIsActiveTrue(String tenantCode);
    List<Dictionary> findByTenantCodeAndTermContainingIgnoreCaseAndIsActiveTrue(String tenantCode, String term);
    boolean existsByTenantCodeAndTypeAndTerm(String tenantCode, String type, String term);
}
