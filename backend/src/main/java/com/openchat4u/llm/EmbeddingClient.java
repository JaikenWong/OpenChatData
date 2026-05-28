package com.openchat4u.llm;

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
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EmbeddingClient {

    @Value("${embedding.base-url:}")
    private String url;

    @Value("${embedding.api-key:}")
    private String apiKey;

    @Value("${embedding.model:}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Float> embed(String text) {
        List<List<Float>> all = embedBatch(List.of(text));
        return all.isEmpty() ? List.of() : all.get(0);
    }

    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", texts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                throw new RuntimeException("Embedding response missing data array. Raw: " + response.getBody());
            }
            List<List<Float>> result = new ArrayList<>(data.size());
            for (JsonNode item : data) {
                JsonNode emb = item.path("embedding");
                List<Float> vec = new ArrayList<>(emb.size());
                for (JsonNode v : emb) {
                    vec.add(v.floatValue());
                }
                result.add(vec);
            }
            log.info("Embedding ok: model={}, inputs={}, dim={}", model, texts.size(),
                result.isEmpty() ? 0 : result.get(0).size());
            return result;
        } catch (Exception e) {
            log.error("Embedding call failed. url={}, model={}", url, model, e);
            throw new RuntimeException("Embedding failed: " + e.getMessage());
        }
    }
}
