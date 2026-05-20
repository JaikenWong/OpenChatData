package com.openchat4u.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DataSourceRegistry {

    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

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

    public void register(String tenantCode, String dbType, String jdbcUrl, String username, String password) {
        register(tenantCode, dbType, jdbcUrl, username, password, 5, 10000);
    }

    public void register(String tenantCode, String dbType, String jdbcUrl, String username, String password, int maxConnections, int connectionTimeout) {
        if (dataSources.containsKey(tenantCode)) {
            log.warn("DataSource already registered for tenant: {}", tenantCode);
            return;
        }

        DbType dbTypeEnum = DbType.fromString(dbType);
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(dbTypeEnum.getDriverClass());
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setReadOnly(true);
        dataSource.setMaximumPoolSize(maxConnections);
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(connectionTimeout);

        dataSources.put(tenantCode, dataSource);
        log.info("DataSource registered for tenant: {}, dbType: {}", tenantCode, dbType);
    }

    public void remove(String tenantCode) {
        HikariDataSource dataSource = dataSources.remove(tenantCode);
        if (dataSource != null) {
            dataSource.close();
            log.info("DataSource removed for tenant: {}", tenantCode);
        }
    }

    public HikariDataSource getDataSource(String tenantCode) {
        return dataSources.get(tenantCode);
    }

    public Map<String, HikariDataSource> getDataSources() {
        return new ConcurrentHashMap<>(dataSources);
    }

    public void closeAll() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }
}
