package com.openchat4u.schema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实测 QdrantService 对本地 Qdrant (:6333) 的 collection/upsert/search 生命周期。
 * 跳过：本地 Qdrant 不可达。
 */
@EnabledIf("qdrantUp")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QdrantServiceLiveTest {

    private static final String TENANT = "junit_live";
    private static final int DIM = 4;

    private QdrantService svc;

    static boolean qdrantUp() {
        try {
            RestTemplate rt = new RestTemplate();
            rt.exchange("http://localhost:6333/", HttpMethod.GET, null, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    void setup() {
        svc = new QdrantService();
        ReflectionTestUtils.setField(svc, "host", "localhost");
        ReflectionTestUtils.setField(svc, "port", 6333);
        // 清前一次残留
        svc.deleteCollection(TENANT);
    }

    @AfterAll
    void teardown() {
        if (svc != null) svc.deleteCollection(TENANT);
    }

    @Test
    @Order(1)
    void ensureCollection_createsThenIdempotent() {
        svc.ensureCollection(TENANT, DIM);
        svc.ensureCollection(TENANT, DIM); // second call must not throw
    }

    @Test
    @Order(2)
    void upsertAndSearch_returnsClosestPoint() {
        List<QdrantService.Point> points = List.of(
            QdrantService.Point.of(null, List.of(1.0f, 0.0f, 0.0f, 0.0f), Map.of("name", "x-axis")),
            QdrantService.Point.of(null, List.of(0.0f, 1.0f, 0.0f, 0.0f), Map.of("name", "y-axis")),
            QdrantService.Point.of(null, List.of(0.0f, 0.0f, 1.0f, 0.0f), Map.of("name", "z-axis"))
        );
        svc.upsertPoints(TENANT, points);

        List<QdrantService.SearchHit> hits = svc.search(TENANT, List.of(0.9f, 0.1f, 0.0f, 0.0f), 2);
        assertNotNull(hits);
        assertFalse(hits.isEmpty());
        assertEquals("x-axis", hits.get(0).payload.get("name"));
        assertTrue(hits.get(0).score >= hits.get(hits.size() - 1).score);
        System.out.println("[qdrant] top=" + hits.get(0).payload + " score=" + hits.get(0).score);
    }

    @Test
    @Order(3)
    void search_missingCollection_returnsEmpty() {
        List<QdrantService.SearchHit> hits = svc.search("definitely_not_exists_xyz", List.of(1f, 0f, 0f, 0f), 5);
        assertNotNull(hits);
        assertTrue(hits.isEmpty());
    }
}
