package com.openchat4u.util;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbTypeUtil {
    
    public enum DbType {
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://"),
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
        ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@"),
        SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://");

        private final String driverClass;
        private final String urlPrefix;

        DbType(String driverClass, String urlPrefix) {
            this.driverClass = driverClass;
            this.urlPrefix = urlPrefix;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public String getUrlPrefix() {
            return urlPrefix;
        }

        public static DbType fromString(String type) {
            for (DbType dbType : values()) {
                if (dbType.name().equalsIgnoreCase(type)) {
                    return dbType;
                }
            }
            throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }

    public static DbType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return DbType.POSTGRESQL;
        }
        
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return DbType.POSTGRESQL;
        }
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return DbType.MYSQL;
        }
        if (jdbcUrl.startsWith("jdbc:oracle:")) {
            return DbType.ORACLE;
        }
        if (jdbcUrl.startsWith("jdbc:sqlserver:")) {
            return DbType.SQLSERVER;
        }
        
        log.warn("Unknown database type for JDBC URL: {}", jdbcUrl);
        return DbType.POSTGRESQL;
    }

    public static DbType fromDataSource(HikariDataSource dataSource) {
        if (dataSource == null) {
            return DbType.POSTGRESQL;
        }
        return fromJdbcUrl(dataSource.getJdbcUrl());
    }

    public static String getLimitClause(DbType dbType, int limit) {
        return switch (dbType) {
            case ORACLE -> " FETCH FIRST " + limit + " ROWS ONLY";
            case SQLSERVER -> " TOP " + limit;
            default -> " LIMIT " + limit;
        };
    }

    public static boolean supportsLimit(DbType dbType) {
        return true;
    }

    public static String applyLimit(String sql, DbType dbType, int limit) {
        String upperSql = sql.toUpperCase().trim();
        
        if (upperSql.contains(" LIMIT ") || 
            upperSql.contains(" FETCH FIRST ") || 
            upperSql.contains(" TOP ") ||
            upperSql.contains(" ROWNUM ")) {
            return sql;
        }

        return switch (dbType) {
            case ORACLE -> sql + " FETCH FIRST " + limit + " ROWS ONLY";
            case SQLSERVER -> {
                if (upperSql.startsWith("SELECT")) {
                    yield sql.replaceFirst("(?i)\\bSELECT\\b", "SELECT TOP " + limit);
                } else {
                    yield sql + " FETCH FIRST " + limit + " ROWS ONLY";
                }
            }
            default -> sql + " LIMIT " + limit;
        };
    }
}
