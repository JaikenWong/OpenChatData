package com.openchat4u.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RerankService {

    @Value("${rerank.enabled:false}")
    private boolean enabled;

    @Value("${rerank.api-key:}")
    private String rerankApiKey;

    @Value("${rerank.base-url:https://maas-api.cn-huabei-1.xf-yun.com/v2/rerank}")
    private String rerankBaseUrl;

    @Value("${rerank.model:}")
    private String rerankModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 对检索到的表结构进行重排序，返回最相关的表
     * @param query 用户问题
     * @param documents 表结构文本列表
     * @return 重排序后的结果，按相关性降序
     */
    public List<RerankResult> rerank(String query, List<String> documents) {
        if (!enabled) {
            return fallbackOriginalOrder(documents);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + rerankApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", rerankModel);
            body.put("query", query);
            body.put("documents", documents);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(rerankBaseUrl, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");

            List<RerankResult> rerankResults = new ArrayList<>();
            for (Map<String, Object> result : results) {
                RerankResult r = new RerankResult();
                r.setIndex((Integer) result.get("index"));
                r.setRelevanceScore(((Number) result.get("relevance_score")).doubleValue());
                Object textVal = result.get("text");
                if (textVal == null) {
                    Object doc = result.get("document");
                    if (doc instanceof Map<?, ?> docMap) {
                        textVal = docMap.get("text");
                    } else if (doc instanceof String docStr) {
                        textVal = docStr;
                    }
                }
                r.setText(textVal != null ? textVal.toString() : null);
                rerankResults.add(r);
            }

            // 按相关性降序排序
            rerankResults.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            return rerankResults;

        } catch (Exception e) {
            log.warn("Rerank failed: {}", e.getMessage());
            return fallbackOriginalOrder(documents);
        }
    }

    private List<RerankResult> fallbackOriginalOrder(List<String> documents) {
        List<RerankResult> fallback = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            RerankResult r = new RerankResult();
            r.setIndex(i);
            r.setRelevanceScore(0.0);
            r.setText(documents.get(i));
            fallback.add(r);
        }
        return fallback;
    }

    public static class RerankResult {
        private Integer index;
        private Double relevanceScore;
        private String text;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public Double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
