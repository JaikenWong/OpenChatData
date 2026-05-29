package com.openchat4u.llm;

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
 * 实测 embedding API（讯飞 MaaS）。
 * 跳过：缺 EMBEDDING_API_KEY。
 */
@EnabledIf("hasKey")
class EmbeddingClientLiveTest {

    static boolean hasKey() {
        return DotEnv.has("EMBEDDING_API_KEY");
    }

    private EmbeddingClient client() {
        EmbeddingClient c = new EmbeddingClient();
        ReflectionTestUtils.setField(c, "url",
            DotEnv.getOr("EMBEDDING_BASE_URL", "https://maas-api.cn-huabei-1.xf-yun.com/v2/embeddings"));
        ReflectionTestUtils.setField(c, "apiKey", DotEnv.get("EMBEDDING_API_KEY"));
        ReflectionTestUtils.setField(c, "model",
            DotEnv.getOr("EMBEDDING_MODEL", "xop3qwen8bembedding"));
        return c;
    }

    @Test
    void embed_singleText_returnsNonEmptyVector() {
        List<Float> vec = client().embed("商品信息查询");
        assertNotNull(vec);
        assertFalse(vec.isEmpty());
        assertTrue(vec.size() >= 256);
        System.out.println("[embed] dim=" + vec.size() + " head=" + vec.subList(0, 5));
    }

    @Test
    void embedBatch_multipleTexts_returnsSameSizeAndConsistentDim() {
        List<String> inputs = List.of(
            "查询北京的销售数据",
            "Show me total revenue last month",
            "产品库存按颜色统计"
        );
        List<List<Float>> all = client().embedBatch(inputs);
        assertEquals(inputs.size(), all.size());
        int dim = all.get(0).size();
        assertTrue(dim > 0);
        for (int i = 0; i < all.size(); i++) {
            assertEquals(dim, all.get(i).size());
        }
        System.out.println("[batch] n=" + all.size() + " dim=" + dim);
    }

    @Test
    void embedBatch_emptyList_returnsEmpty() {
        assertTrue(client().embedBatch(List.of()).isEmpty());
    }
}
