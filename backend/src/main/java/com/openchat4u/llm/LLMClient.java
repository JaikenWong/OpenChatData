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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LLMClient {

    @Value("${llm.base-url:}")
    private String baseUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String prompt, String systemPrompt) {
        return chat(prompt, systemPrompt, null);
    }

    public String chat(String prompt, String systemPrompt, String overrideModel) {
        String actualModel = overrideModel != null ? overrideModel : model;
        String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions";

        Map<String, Object> body = Map.of(
            "model", actualModel,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "You are a helpful assistant."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 2000,
            "temperature", 0.1
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new RuntimeException("LLM response missing content. Raw: " + response.getBody());
            }
            return content.asText().trim();
        } catch (Exception e) {
            log.error("LLM chat failed with model: {}, url: {}", actualModel, url, e);
            throw new RuntimeException("LLM chat failed: " + e.getMessage());
        }
    }

    public String generateSQL(String question, String schemaContext, String dbType) {
        String dialectHint = switch (dbType) {
            case "POSTGRESQL" -> "Use PostgreSQL syntax.";
            case "MYSQL" -> "Use MySQL syntax.";
            case "ORACLE" -> "Use Oracle SQL syntax.";
            case "SQLSERVER" -> "Use SQL Server (T-SQL) syntax.";
            default -> "Use standard SQL syntax.";
        };

        String prompt = """
You are a SQL expert. Convert the natural language question to a SQL query.
Only use the tables provided in the Schema section.
Use the exact literal values shown in "Sample rows" — do not invent attribute names or codes.

EAV guidance (entity / attribute-definition / attribute-value tables):
- To filter rows by a specific attribute, put the attribute-name condition in WHERE,
  or use INNER JOIN. NEVER put it only in a LEFT JOIN ... ON clause (that does not filter).
- To return one attribute filtered by another attribute of the same entity, self-join
  the value table once per attribute. Example:
    SELECT vp.value AS packaging_size
    FROM attribute_values vc
    JOIN attribute_defs dc ON vc.attr_def_id = dc.id AND dc.attr_name = 'code'
    JOIN attribute_values vp ON vp.product_id = vc.product_id
    JOIN attribute_defs dp ON vp.attr_def_id = dp.id AND dp.attr_name = 'packaging_size'
    WHERE vc.value = '12312'
- If the filter attribute is a plain column on the entity table, filter it there directly
  and join the value table only for the wanted attribute.
- If the question gives a bare value (e.g. an id/code/SKU like "12312") WITHOUT naming
  which attribute it is, locate the entity by matching that value across ANY attribute
  (filter only on the value, do NOT constrain the filter attribute's name), then return
  the requested attribute. Example:
    SELECT vp.value
    FROM attribute_values vc
    JOIN attribute_values vp ON vp.product_id = vc.product_id
    JOIN attribute_defs dp ON vp.attr_def_id = dp.id AND dp.attr_name = 'packaging_size'
    WHERE vc.value = '12312'

Schema:
%s

Question: %s

Return ONLY the SQL query, no explanation, no markdown code fences. %s Always use LIMIT 1000 unless specified otherwise.
""".formatted(schemaContext, question, dialectHint);

        String raw = chat(prompt, "You are a SQL expert. Convert natural language questions to database queries.", null);
        return extractSql(raw);
    }

    static String extractSql(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        java.util.regex.Matcher fence = java.util.regex.Pattern
            .compile("```(?:sql|SQL)?\\s*([\\s\\S]*?)```")
            .matcher(s);
        if (fence.find()) {
            s = fence.group(1).trim();
        }
        if (s.toLowerCase().startsWith("sql\n") || s.toLowerCase().startsWith("sql ")) {
            s = s.substring(3).trim();
        }
        int semi = s.indexOf(';');
        if (semi >= 0) {
            s = s.substring(0, semi);
        }
        return s.trim();
    }

    public String generateAnswer(String question, String sql, String dataSummary) {
        String prompt = """
You are a data analyst. Answer the user's question based on the SQL query result.

Question: %s
SQL: %s
%s

Provide a concise, natural language answer.
""".formatted(question, sql, dataSummary);

        return chat(prompt, "You are a data analyst. Answer questions based on SQL query results.", null);
    }
}
