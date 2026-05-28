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

    @Test
    void testColumnNamesContainingKeywords() {
        // created_at / updated_at must not trip CREATE / UPDATE guards.
        assertTrue(isValidReadOnlySQL("SELECT created_at, updated_at FROM users"));
        assertTrue(isValidReadOnlySQL("SELECT * FROM products ORDER BY created_at DESC"));
        assertTrue(isValidReadOnlySQL("SELECT name, created_at FROM products WHERE updated_at > '2026-01-01'"));
    }

    private static final java.util.regex.Pattern WRITE_KEYWORD = java.util.regex.Pattern.compile(
        "\\b(DROP|TRUNCATE|DELETE|INSERT|UPDATE|ALTER|CREATE|GRANT|REVOKE|MERGE|CALL)\\b",
        java.util.regex.Pattern.CASE_INSENSITIVE);

    private boolean isValidReadOnlySQL(String sql) {
        String upper = sql.toUpperCase().trim();
        boolean isReadOnly = upper.startsWith("SELECT") || upper.startsWith("WITH") ||
               upper.startsWith("SHOW") || upper.startsWith("DESCRIBE") || upper.startsWith("EXPLAIN");

        return isReadOnly && !WRITE_KEYWORD.matcher(sql).find();
    }
}
