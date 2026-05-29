package com.openchat4u.llm;

import com.openchat4u.testutil.DotEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实测 LLMClient（讯飞 astron-code）。
 * 跳过：缺 API_KEY。
 */
@EnabledIf("hasKey")
class LLMClientLiveTest {

    static boolean hasKey() {
        return DotEnv.has("API_KEY");
    }

    private LLMClient client() {
        LLMClient c = new LLMClient();
        ReflectionTestUtils.setField(c, "baseUrl",
            DotEnv.getOr("BASE_URL", "https://maas-coding-api.cn-huabei-1.xf-yun.com/v2"));
        ReflectionTestUtils.setField(c, "apiKey", DotEnv.get("API_KEY"));
        ReflectionTestUtils.setField(c, "model",
            DotEnv.getOr("MODEL_ID", "astron-code-latest"));
        return c;
    }

    @Test
    void chat_returnsNonEmptyAnswer() {
        String reply = client().chat("Reply with the single word: pong", "You are concise.");
        assertNotNull(reply);
        assertFalse(reply.isBlank());
        System.out.println("[chat] " + reply);
    }

    @Test
    void generateSQL_simpleQuestion_producesSelect() {
        String schema = """
            Table: customers
              id BIGINT
              name VARCHAR
              city VARCHAR
              created_at TIMESTAMP
            Table: orders
              id BIGINT
              customer_id BIGINT
              amount DECIMAL
              created_at TIMESTAMP
            """;
        String sql = client().generateSQL("北京客户的订单总金额", schema, "MYSQL");
        assertNotNull(sql);
        assertFalse(sql.isBlank());
        String lower = sql.toLowerCase();
        assertTrue(lower.contains("select"), "missing select: " + sql);
        assertTrue(lower.contains("customers") || lower.contains("orders"), "no expected tables: " + sql);
        System.out.println("[sql] " + sql);
    }

    @Test
    void extractSql_stripsCodeFence() {
        String raw = "```sql\nSELECT 1;\n```";
        assertEquals("SELECT 1", LLMClient.extractSql(raw));
    }

    @Test
    void extractSql_handlesPlainSqlWithTrailingSemicolon() {
        assertEquals("SELECT * FROM t", LLMClient.extractSql("SELECT * FROM t;"));
    }
}
