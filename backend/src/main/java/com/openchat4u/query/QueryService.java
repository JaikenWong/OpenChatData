package com.openchat4u.query;

import com.openchat4u.datasource.DataSourceRegistry;
import com.openchat4u.llm.LLMClient;
import com.openchat4u.schema.SchemaService;
import com.openchat4u.schema.TableSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final DataSourceRegistry dataSourceRegistry;
    private final SchemaService schemaService;
    private final LLMClient llmClient;
    private final RerankService rerankService;

    private static final int VECTOR_TOPK = 10;
    private static final int RERANK_TOPK = 4;

    public QueryResponse ask(String tenantCode, QueryRequest request) {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(tenantCode);
            if (jdbcTemplate == null) {
                return QueryResponse.error("DataSource not found for tenant: " + tenantCode);
            }

            String dbType = getDbType(tenantCode);
            String schemaContext = buildSchemaContext(tenantCode, request.getQuestion(), request.getTables());

            // 使用 LLM 生成 SQL
            String sql = llmClient.generateSQL(request.getQuestion(), schemaContext, dbType);

            // 验证 SQL
            if (!isValidReadOnlySQL(sql)) {
                return QueryResponse.error("SQL validation failed: only SELECT statements are allowed");
            }

            // 执行 SQL
            List<Map<String, Object>> data = executeSQL(jdbcTemplate, sql);

            // 生成自然语言回答
            String answer = llmClient.generateAnswer(request.getQuestion(), sql, summarizeData(data));

            return QueryResponse.success(answer, sql, data);

        } catch (Exception e) {
            log.error("Query failed", e);
            return QueryResponse.error("Query failed: " + e.getMessage());
        }
    }

    private String buildSchemaContext(String tenantCode, String question, List<String> userTables) {
        List<String> tables;
        if (userTables != null && !userTables.isEmpty()) {
            tables = userTables;
        } else {
            tables = retrieveRelevantTables(tenantCode, question);
        }

        StringBuilder sb = new StringBuilder();
        for (String tableName : tables) {
            try {
                TableSchema schema = schemaService.getTableSchema(tenantCode, tableName);
                sb.append(schemaToString(schema));
            } catch (Exception e) {
                log.warn("Could not load schema for table: {}", tableName);
            }
        }
        return sb.toString();
    }

    private List<String> retrieveRelevantTables(String tenantCode, String question) {
        List<String> initial;
        try {
            initial = schemaService.searchRelevantTables(tenantCode, question, VECTOR_TOPK);
        } catch (Exception e) {
            log.warn("Vector retrieval failed, fallback to all tables: {}", e.getMessage());
            initial = schemaService.getTables(tenantCode);
        }
        if (initial.isEmpty()) {
            initial = schemaService.getTables(tenantCode);
        }
        final List<String> candidates = initial;
        if (candidates.size() <= RERANK_TOPK) {
            return candidates;
        }
        if (!rerankService.isEnabled()) {
            return candidates.subList(0, RERANK_TOPK);
        }

        List<String> docs = new ArrayList<>();
        for (String table : candidates) {
            try {
                docs.add(schemaToString(schemaService.getTableSchema(tenantCode, table)));
            } catch (Exception ignore) {
                docs.add(table);
            }
        }
        try {
            List<RerankService.RerankResult> ranked = rerankService.rerank(question, docs);
            return ranked.stream()
                .limit(RERANK_TOPK)
                .map(r -> candidates.get(r.getIndex()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Rerank failed, using vector top-{}: {}", RERANK_TOPK, e.getMessage());
            return candidates.subList(0, RERANK_TOPK);
        }
    }

    private String schemaToString(TableSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(schema.getTableName());
        if (schema.getTableComment() != null) {
            sb.append(" (").append(schema.getTableComment()).append(")");
        }
        sb.append("\nColumns:\n");
        for (TableSchema.ColumnSchema col : schema.getColumns()) {
            sb.append("  - ").append(col.getColumnName())
              .append(": ").append(col.getDataType());
            if (col.getColumnComment() != null) {
                sb.append(" (").append(col.getColumnComment()).append(")");
            }
            sb.append("\n");
        }
        SchemaService.appendSampleRows(sb, schema);
        sb.append("\n");
        return sb.toString();
    }

    // Word-boundary match so column names like created_at / updated_at don't
    // trip the CREATE / UPDATE guards.
    private static final java.util.regex.Pattern WRITE_KEYWORD = java.util.regex.Pattern.compile(
        "\\b(DROP|TRUNCATE|DELETE|INSERT|UPDATE|ALTER|CREATE|GRANT|REVOKE|MERGE|CALL)\\b",
        java.util.regex.Pattern.CASE_INSENSITIVE);

    private boolean isValidReadOnlySQL(String sql) {
        String upper = sql.toUpperCase().trim();
        boolean isReadOnly = upper.startsWith("SELECT") || upper.startsWith("WITH") ||
               upper.startsWith("SHOW") || upper.startsWith("DESCRIBE") || upper.startsWith("EXPLAIN");

        return isReadOnly && !WRITE_KEYWORD.matcher(sql).find();
    }

    private List<Map<String, Object>> executeSQL(JdbcTemplate jdbcTemplate, String sql) {
        String cleaned = sql.trim();
        while (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        String upper = cleaned.toUpperCase();
        String limitedSql = cleaned;
        if (!upper.contains("LIMIT") && !upper.contains("ROWNUM") && !upper.contains("FETCH FIRST") && !upper.contains("TOP ")) {
            limitedSql = cleaned + " LIMIT 1000";
        }
        return jdbcTemplate.queryForList(limitedSql);
    }

    private String summarizeData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "No data returned";
        }
        
        int size = data.size();
        String sample = data.stream().limit(3).map(Object::toString).reduce((a, b) -> a + "\n" + b).orElse("");
        
        return String.format("Result: %d rows. Sample:\n%s", size, sample);
    }

    private JdbcTemplate getJdbcTemplate(String tenantCode) {
        var dataSource = dataSourceRegistry.getDataSource(tenantCode);
        if (dataSource == null) {
            return null;
        }
        return new JdbcTemplate(dataSource);
    }

    private String getDbType(String tenantCode) {
        var dataSource = dataSourceRegistry.getDataSource(tenantCode);
        if (dataSource == null) {
            return "POSTGRESQL";
        }
        String jdbcUrl = dataSource.getJdbcUrl();
        if (jdbcUrl.startsWith("jdbc:postgresql:")) return "POSTGRESQL";
        if (jdbcUrl.startsWith("jdbc:mysql:")) return "MYSQL";
        if (jdbcUrl.startsWith("jdbc:oracle:")) return "ORACLE";
        if (jdbcUrl.startsWith("jdbc:sqlserver:")) return "SQLSERVER";
        return "POSTGRESQL";
    }
}
