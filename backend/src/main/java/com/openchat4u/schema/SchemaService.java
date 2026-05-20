package com.openchat4u.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.openchat4u.datasource.DataSourceRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final DataSourceRegistry dataSourceRegistry;
    private final ObjectMapper objectMapper;

    public List<String> getTables(String tenantCode) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(tenantCode);
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("DataSource not found for tenant: " + tenantCode);
        }

        String dbType = getDbType(tenantCode);
        String sql = getTablesSql(dbType);
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public TableSchema getTableSchema(String tenantCode, String tableName) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(tenantCode);
        String dbType = getDbType(tenantCode);

        String tableSql = getTableCommentSql(dbType);
        String comment = null;
        try {
            comment = jdbcTemplate.queryForObject(tableSql, String.class, tableName);
        } catch (Exception e) {
            log.warn("Could not get table comment for {}: {}", tableName, e.getMessage());
        }

        String columnSql = getColumnsSql(dbType);
        List<TableSchema.ColumnSchema> columns = jdbcTemplate.query(columnSql, (rs, rowNum) -> {
            TableSchema.ColumnSchema col = new TableSchema.ColumnSchema();
            col.setColumnName(rs.getString("column_name"));
            col.setDataType(rs.getString("data_type"));
            col.setColumnComment(rs.getString("column_comment"));
            col.setNullable("YES".equals(rs.getString("is_nullable")));
            return col;
        }, tableName);

        TableSchema schema = new TableSchema();
        schema.setTableName(tableName);
        schema.setTableComment(comment);
        schema.setColumns(columns);

        return schema;
    }

    public void syncSchemaToQdrant(String tenantCode, List<String> tableNames) {
        // TODO: 实现 Qdrant 向量检索集成
        log.info("Schema sync requested for tenant: {}, tables: {}", tenantCode, tableNames);
        log.warn("Qdrant integration is pending - using simplified schema storage");
    }

    private String getDbType(String tenantCode) {
        var dataSource = dataSourceRegistry.getDataSource(tenantCode);
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource not found for tenant: " + tenantCode);
        }
        String jdbcUrl = dataSource.getJdbcUrl();
        if (jdbcUrl.startsWith("jdbc:postgresql:")) return "POSTGRESQL";
        if (jdbcUrl.startsWith("jdbc:mysql:")) return "MYSQL";
        if (jdbcUrl.startsWith("jdbc:oracle:")) return "ORACLE";
        if (jdbcUrl.startsWith("jdbc:sqlserver:")) return "SQLSERVER";
        return "POSTGRESQL";
    }

    private String getTablesSql(String dbType) {
        return switch (dbType) {
            case "POSTGRESQL" -> "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
            case "MYSQL" -> "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
            case "ORACLE" -> "SELECT table_name FROM user_tables";
            case "SQLSERVER" -> "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";
            default -> throw new IllegalArgumentException("Unsupported db type: " + dbType);
        };
    }

    private String getTableCommentSql(String dbType) {
        return switch (dbType) {
            case "POSTGRESQL" -> "SELECT obj_description((quote_ident('public') || '.' || quote_ident(?))::regclass, 'class') as comment";
            case "MYSQL" -> "SELECT TABLE_COMMENT FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            case "ORACLE" -> "SELECT COMMENTS FROM user_tab_comments WHERE table_name = ?";
            case "SQLSERVER" -> "SELECT CAST(ep.value AS NVARCHAR(MAX)) FROM sys.tables t LEFT JOIN sys.extended_properties ep ON t.object_id = ep.major_id AND ep.minor_id = 0 AND ep.name = 'MS_Description' WHERE t.name = ?";
            default -> throw new IllegalArgumentException("Unsupported db type: " + dbType);
        };
    }

    private String getColumnsSql(String dbType) {
        return switch (dbType) {
            case "POSTGRESQL" -> """
                SELECT column_name, data_type, 
                       COALESCE(pg_catalog.col_description(
                           ('public.' || quote_ident(?))::regclass::oid, 
                           ordinal_position::integer
                       ), '') as column_comment,
                       is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                ORDER BY ordinal_position
                """;
            case "MYSQL" -> """
                SELECT column_name, data_type, column_comment, is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                ORDER BY ordinal_position
                """;
            case "ORACLE" -> """
                SELECT col.column_name, col.data_type, comm.comments as column_comment,
                       CASE WHEN col.nullable = 'Y' THEN 'YES' ELSE 'NO' END as is_nullable
                FROM user_tab_columns col
                LEFT JOIN user_col_comments comm ON col.table_name = comm.table_name AND col.column_name = comm.column_name
                WHERE col.table_name = ?
                ORDER BY col.column_id
                """;
            case "SQLSERVER" -> """
                SELECT c.COLUMN_NAME, c.DATA_TYPE,
                       CAST(ep.value AS NVARCHAR(MAX)) as column_comment,
                       c.IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS c
                LEFT JOIN sys.columns sc ON sc.name = c.COLUMN_NAME
                LEFT JOIN sys.tables t ON t.object_id = sc.object_id
                LEFT JOIN sys.extended_properties ep ON ep.major_id = t.object_id AND ep.minor_id = sc.column_id AND ep.name = 'MS_Description'
                WHERE c.TABLE_NAME = ?
                ORDER BY c.ORDINAL_POSITION
                """;
            default -> throw new IllegalArgumentException("Unsupported db type: " + dbType);
        };
    }

    private String buildSchemaText(TableSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(schema.getTableName());
        if (schema.getTableComment() != null) {
            sb.append(" Comment: ").append(schema.getTableComment());
        }
        sb.append("\nColumns:\n");
        for (TableSchema.ColumnSchema col : schema.getColumns()) {
            sb.append("  - ").append(col.getColumnName())
              .append(" (").append(col.getDataType()).append(")");
            if (col.getColumnComment() != null) {
                sb.append(": ").append(col.getColumnComment());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private JdbcTemplate getJdbcTemplate(String tenantCode) {
        var dataSource = dataSourceRegistry.getDataSource(tenantCode);
        if (dataSource == null) {
            return null;
        }
        return new JdbcTemplate(dataSource);
    }
}
