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

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final DataSourceRegistry dataSourceRegistry;
    private final SchemaService schemaService;
    private final LLMClient llmClient;

    public QueryResponse ask(String tenantCode, QueryRequest request) {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(tenantCode);
            if (jdbcTemplate == null) {
                return QueryResponse.error("DataSource not found for tenant: " + tenantCode);
            }

            String dbType = getDbType(tenantCode);
            String schemaContext = buildSchemaContext(tenantCode, request.getTables());

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

    private String buildSchemaContext(String tenantCode, List<String> tables) {
        if (tables == null || tables.isEmpty()) {
            // TODO: 实现向量检索，当前返回所有表
            tables = schemaService.getTables(tenantCode);
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
        sb.append("\n");
        return sb.toString();
    }

    private boolean isValidReadOnlySQL(String sql) {
        String upper = sql.toUpperCase().trim();
        boolean isReadOnly = upper.startsWith("SELECT") || upper.startsWith("WITH") ||
               upper.startsWith("SHOW") || upper.startsWith("DESCRIBE") || upper.startsWith("EXPLAIN");
        
        return isReadOnly && !upper.contains("DROP") && !upper.contains("TRUNCATE") && 
               !upper.contains("DELETE") && !upper.contains("INSERT") && !upper.contains("UPDATE") &&
               !upper.contains("ALTER") && !upper.contains("CREATE");
    }

    private List<Map<String, Object>> executeSQL(JdbcTemplate jdbcTemplate, String sql) {
        String limitedSql = sql;
        String upper = sql.toUpperCase();
        if (!upper.contains("LIMIT") && !upper.contains("ROWNUM") && !upper.contains("FETCH FIRST") && !upper.contains("TOP")) {
            limitedSql = sql + " LIMIT 1000";
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
