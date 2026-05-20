package com.openchat4u.datasource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class DataSourceRegistryTest {

    private DataSourceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DataSourceRegistry();
    }

    @AfterEach
    void tearDown() {
        registry.closeAll();
    }

    @Test
    void testRegisterPostgreSQLDataSource() {
        registry.register("tenant1", "POSTGRESQL", "jdbc:postgresql://localhost:5432/testdb", "user", "pass");
        
        var dataSource = registry.getDataSource("tenant1");
        assertNotNull(dataSource);
        assertEquals("jdbc:postgresql://localhost:5432/testdb", dataSource.getJdbcUrl());
        assertTrue(dataSource.isReadOnly());
    }

    @Test
    void testRegisterMySQLDataSource() {
        registry.register("tenant2", "MYSQL", "jdbc:mysql://localhost:3306/testdb", "user", "pass");
        
        var dataSource = registry.getDataSource("tenant2");
        assertNotNull(dataSource);
        assertEquals("jdbc:mysql://localhost:3306/testdb", dataSource.getJdbcUrl());
        assertEquals("com.mysql.cj.jdbc.Driver", dataSource.getDriverClassName());
    }

    @Test
    void testRegisterOracleDataSource() {
        registry.register("tenant3", "ORACLE", "jdbc:oracle:thin:@localhost:1521:orcl", "user", "pass");
        
        var dataSource = registry.getDataSource("tenant3");
        assertNotNull(dataSource);
        assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", dataSource.getJdbcUrl());
        assertEquals("oracle.jdbc.OracleDriver", dataSource.getDriverClassName());
    }

    @Test
    void testRegisterSQLServerDataSource() {
        registry.register("tenant4", "SQLSERVER", "jdbc:sqlserver://localhost:1433;databaseName=testdb", "user", "pass");
        
        var dataSource = registry.getDataSource("tenant4");
        assertNotNull(dataSource);
        assertEquals("jdbc:sqlserver://localhost:1433;databaseName=testdb", dataSource.getJdbcUrl());
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", dataSource.getDriverClassName());
    }

    @Test
    void testRegisterWithCustomPoolSettings() {
        registry.register("tenant5", "POSTGRESQL", "jdbc:postgresql://localhost:5432/testdb", "user", "pass", 10, 5000);
        
        var dataSource = registry.getDataSource("tenant5");
        assertNotNull(dataSource);
        assertEquals(10, dataSource.getMaximumPoolSize());
        assertEquals(5000, dataSource.getConnectionTimeout());
    }

    @Test
    void testRegisterDuplicateDataSource() {
        registry.register("tenant1", "POSTGRESQL", "jdbc:postgresql://localhost:5432/testdb", "user", "pass");
        registry.register("tenant1", "MYSQL", "jdbc:mysql://localhost:3306/testdb", "user", "pass");
        
        var dataSource = registry.getDataSource("tenant1");
        assertNotNull(dataSource);
        assertEquals("jdbc:postgresql://localhost:5432/testdb", dataSource.getJdbcUrl());
        assertEquals(1, registry.getDataSources().size());
    }

    @Test
    void testRemoveDataSource() {
        registry.register("tenant1", "POSTGRESQL", "jdbc:postgresql://localhost:5432/testdb", "user", "pass");
        assertNotNull(registry.getDataSource("tenant1"));
        
        registry.remove("tenant1");
        assertNull(registry.getDataSource("tenant1"));
        assertTrue(registry.getDataSources().isEmpty());
    }

    @Test
    void testRemoveNonExistentDataSource() {
        assertDoesNotThrow(() -> registry.remove("nonexistent"));
    }

    @Test
    void testGetNonExistentDataSource() {
        assertNull(registry.getDataSource("nonexistent"));
    }

    @Test
    void testCloseAllDataSources() {
        registry.register("tenant1", "POSTGRESQL", "jdbc:postgresql://localhost:5432/db1", "user", "pass");
        registry.register("tenant2", "MYSQL", "jdbc:mysql://localhost:3306/db2", "user", "pass");
        
        assertEquals(2, registry.getDataSources().size());
        
        registry.closeAll();
        assertTrue(registry.getDataSources().isEmpty());
    }

    @Test
    void testDbTypeEnum() {
        assertEquals("org.postgresql.Driver", DataSourceRegistry.DbType.POSTGRESQL.getDriverClass());
        assertEquals("com.mysql.cj.jdbc.Driver", DataSourceRegistry.DbType.MYSQL.getDriverClass());
        assertEquals("oracle.jdbc.OracleDriver", DataSourceRegistry.DbType.ORACLE.getDriverClass());
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", DataSourceRegistry.DbType.SQLSERVER.getDriverClass());
        
        assertEquals("jdbc:postgresql://", DataSourceRegistry.DbType.POSTGRESQL.getUrlPrefix());
        assertEquals("jdbc:mysql://", DataSourceRegistry.DbType.MYSQL.getUrlPrefix());
    }

    @Test
    void testDbTypeFromString() {
        assertEquals(DataSourceRegistry.DbType.POSTGRESQL, DataSourceRegistry.DbType.fromString("POSTGRESQL"));
        assertEquals(DataSourceRegistry.DbType.MYSQL, DataSourceRegistry.DbType.fromString("mysql"));
        assertEquals(DataSourceRegistry.DbType.ORACLE, DataSourceRegistry.DbType.fromString("Oracle"));
        assertEquals(DataSourceRegistry.DbType.SQLSERVER, DataSourceRegistry.DbType.fromString("sqlserver"));
    }

    @Test
    void testDbTypeFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DataSourceRegistry.DbType.fromString("INVALID"));
    }

    @Test
    void testGetDataSourcesReturnsCopy() {
        registry.register("tenant1", "POSTGRESQL", "jdbc:postgresql://localhost:5432/testdb", "user", "pass");
        
        var copy1 = registry.getDataSources();
        var copy2 = registry.getDataSources();
        
        assertNotSame(copy1, copy2);
    }
}
