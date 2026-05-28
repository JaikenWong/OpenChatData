package com.openchat4u.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class QdrantService {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.http-port:6333}")
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl() {
        return "http://" + host + ":" + port;
    }

    public String collectionName(String tenantCode) {
        return "tenant_" + tenantCode + "_schema";
    }

    public void ensureCollection(String tenantCode, int vectorSize) {
        String name = collectionName(tenantCode);
        String url = baseUrl() + "/collections/" + name;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            log.debug("Qdrant collection exists: {}", name);
        } catch (HttpClientErrorException.NotFound e) {
            Map<String, Object> body = Map.of(
                "vectors", Map.of("size", vectorSize, "distance", "Cosine")
            );
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            log.info("Qdrant collection created: {} (vector size {})", name, vectorSize);
        }
    }

    public void upsertPoints(String tenantCode, List<Point> points) {
        if (points.isEmpty()) return;
        String name = collectionName(tenantCode);
        String url = baseUrl() + "/collections/" + name + "/points?wait=true";

        List<Map<String, Object>> rawPoints = new ArrayList<>(points.size());
        for (Point p : points) {
            Map<String, Object> rp = new HashMap<>();
            rp.put("id", p.id != null ? p.id : UUID.randomUUID().toString());
            rp.put("vector", p.vector);
            rp.put("payload", p.payload);
            rawPoints.add(rp);
        }

        Map<String, Object> body = Map.of("points", rawPoints);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        log.info("Qdrant upserted {} points into {}", points.size(), name);
    }

    public List<SearchHit> search(String tenantCode, List<Float> vector, int topK) {
        String name = collectionName(tenantCode);
        String url = baseUrl() + "/collections/" + name + "/points/search";

        Map<String, Object> body = Map.of(
            "vector", vector,
            "limit", topK,
            "with_payload", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode result = root.path("result");
            List<SearchHit> hits = new ArrayList<>();
            for (JsonNode node : result) {
                SearchHit hit = new SearchHit();
                hit.id = node.path("id").asText();
                hit.score = node.path("score").floatValue();
                hit.payload = objectMapper.convertValue(node.path("payload"), Map.class);
                hits.add(hit);
            }
            return hits;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Qdrant collection not found: {}. Run schema sync first.", name);
            return List.of();
        } catch (Exception e) {
            log.error("Qdrant search failed", e);
            return List.of();
        }
    }

    public void deleteCollection(String tenantCode) {
        String name = collectionName(tenantCode);
        try {
            restTemplate.exchange(baseUrl() + "/collections/" + name, HttpMethod.DELETE, null, String.class);
        } catch (Exception e) {
            log.warn("Qdrant delete collection failed: {}", e.getMessage());
        }
    }

    public static class Point {
        public String id;
        public List<Float> vector;
        public Map<String, Object> payload;

        public static Point of(String id, List<Float> vector, Map<String, Object> payload) {
            Point p = new Point();
            p.id = id;
            p.vector = vector;
            p.payload = payload;
            return p;
        }
    }

    public static class SearchHit {
        public String id;
        public float score;
        public Map<String, Object> payload;
    }
}
