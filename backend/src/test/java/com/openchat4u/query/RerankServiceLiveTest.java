package com.openchat4u.query;

import com.openchat4u.testutil.DotEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实测 RerankService。
 * - disabled 模式总跑（passthrough）。
 * - enabled 模式仅在 RERANK_API_KEY + RERANK_MODEL 都有时跑。
 */
class RerankServiceLiveTest {

    private RerankService make(boolean enabled) {
        RerankService r = new RerankService();
        ReflectionTestUtils.setField(r, "enabled", enabled);
        ReflectionTestUtils.setField(r, "rerankApiKey", DotEnv.getOr("RERANK_API_KEY", ""));
        ReflectionTestUtils.setField(r, "rerankBaseUrl",
            DotEnv.getOr("RERANK_BASE_URL", "https://maas-api.cn-huabei-1.xf-yun.com/v2/rerank"));
        ReflectionTestUtils.setField(r, "rerankModel", DotEnv.getOr("RERANK_MODEL", ""));
        return r;
    }

    @Test
    void disabled_returnsDocsInOriginalOrder() {
        RerankService r = make(false);
        List<String> docs = List.of("A", "B", "C");
        List<RerankService.RerankResult> out = r.rerank("anything", docs);
        assertEquals(3, out.size());
        assertEquals("A", out.get(0).getText());
        assertEquals("B", out.get(1).getText());
        assertEquals("C", out.get(2).getText());
        for (RerankService.RerankResult x : out) {
            assertEquals(0.0, x.getRelevanceScore());
        }
    }

    @Test
    @EnabledIf("hasRerankKey")
    void enabled_reordersByRelevance() {
        RerankService r = make(true);
        List<String> docs = List.of(
            "customers table: id, name, email, city",
            "orders table: id, customer_id, amount, created_at",
            "products table: id, sku, price, stock"
        );
        List<RerankService.RerankResult> out = r.rerank("查询某客户的订单总额", docs);
        assertNotNull(out);
        assertFalse(out.isEmpty());
        assertEquals(docs.size(), out.size());
        assertTrue(out.get(0).getRelevanceScore() > 0,
            "top score 0 → remote rerank failed and fell back to passthrough");
        for (int i = 1; i < out.size(); i++) {
            assertTrue(out.get(i - 1).getRelevanceScore() >= out.get(i).getRelevanceScore(),
                "not descending at " + i);
        }
        System.out.println("[rerank] top=" + out.get(0).getText()
            + " score=" + out.get(0).getRelevanceScore());
    }

    static boolean hasRerankKey() {
        return DotEnv.has("RERANK_API_KEY") && DotEnv.has("RERANK_MODEL");
    }
}
