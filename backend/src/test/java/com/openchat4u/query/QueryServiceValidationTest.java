package com.openchat4u.query;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueryServiceValidationTest {

    @Test
    void testValidReadOnlySQL() {
        assertTrue(isValidReadOnlySQL("SELECT * FROM users"));
        assertTrue(isValidReadOnlySQL("select * from users"));
        assertTrue(isValidReadOnlySQL("  SELECT * FROM users  "));
        assertTrue(isValidReadOnlySQL("WITH cte AS (SELECT * FROM users) SELECT * FROM cte"));
        assertTrue(isValidReadOnlySQL("SHOW TABLES"));
        assertTrue(isValidReadOnlySQL("DESCRIBE users"));
        assertTrue(isValidReadOnlySQL("EXPLAIN SELECT * FROM users"));
    }

    @Test
    void testInvalidWriteSQL() {
        assertFalse(isValidReadOnlySQL("INSERT INTO users VALUES (1, 'test')"));
        assertFalse(isValidReadOnlySQL("UPDATE users SET name = 'test'"));
        assertFalse(isValidReadOnlySQL("DELETE FROM users"));
        assertFalse(isValidReadOnlySQL("DROP TABLE users"));
        assertFalse(isValidReadOnlySQL("TRUNCATE TABLE users"));
        assertFalse(isValidReadOnlySQL("ALTER TABLE users ADD COLUMN age INT"));
        assertFalse(isValidReadOnlySQL("CREATE TABLE users (id INT)"));
    }

    @Test
    void testSQLWithDangerousKeywords() {
        assertFalse(isValidReadOnlySQL("SELECT * FROM users; DROP TABLE users"));
        assertFalse(isValidReadOnlySQL("SELECT * FROM users WHERE name = 'test'; DELETE FROM users"));
        assertFalse(isValidReadOnlySQL("SELECT * FROM users; INSERT INTO users VALUES (1, 'test')"));
    }

    @Test
    void testSQLWithLimit() {
        assertTrue(isValidReadOnlySQL("SELECT * FROM users LIMIT 10"));
        assertTrue(isValidReadOnlySQL("SELECT * FROM users WHERE id = 1 LIMIT 1"));
    }

    @Test
    void testEmptySQL() {
        assertFalse(isValidReadOnlySQL(""));
        assertFalse(isValidReadOnlySQL("   "));
    }

    @Test
    void testSQLWithComments() {
        assertTrue(isValidReadOnlySQL("SELECT * FROM users -- comment"));
        assertTrue(isValidReadOnlySQL("SELECT * FROM users /* comment */"));
    }

    private boolean isValidReadOnlySQL(String sql, String dbType) {
        String upper = sql.toUpperCase().trim();
        boolean isReadOnly = upper.startsWith("SELECT") || upper.startsWith("WITH") ||
               upper.startsWith("SHOW") || upper.startsWith("DESCRIBE") || upper.startsWith("EXPLAIN");
        
        if (dbType.equals("ORACLE")) {
            isReadOnly = isReadOnly || upper.startsWith("SELECT") || upper.startsWith("WITH");
        }
        
        return isReadOnly && !upper.contains("DROP") && !upper.contains("TRUNCATE") && 
               !upper.contains("DELETE") && !upper.contains("INSERT") && !upper.contains("UPDATE") &&
               !upper.contains("ALTER") && !upper.contains("CREATE");
    }

    private boolean isValidReadOnlySQL(String sql) {
        return isValidReadOnlySQL(sql, "POSTGRESQL");
    }
}
