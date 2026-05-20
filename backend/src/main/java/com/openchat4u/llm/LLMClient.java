package com.openchat4u.llm;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LLMClient {

    @Value("${llm.provider:local}")
    private String provider;

    @Value("${llm.default-model:kimi}")
    private String defaultModel;

    @Value("${llm.local.base-url:http://10.10.38.188:13000/v1}")
    private String localBaseUrl;

    @Value("${llm.local.api-key:}")
    private String localApiKey;

    @Value("${llm.local.models.kimi:ep-20260109161345-br8vg}")
    private String kimiModel;

    @Value("${llm.local.models.doubao:Doubao-Seed-2.0-Code}")
    private String doubaoModel;

    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${llm.deepseek.api-key:}")
    private String deepSeekApiKey;

    @Value("${llm.deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    public String chat(String prompt, String systemPrompt) {
        return chat(prompt, systemPrompt, null);
    }

    public String chat(String prompt, String systemPrompt, String model) {
        String actualModel = model != null ? model : getDefaultModel();
        OpenAiService service = createService();
        
        try {
            ChatMessage systemMessage = new ChatMessage(
                ChatMessageRole.SYSTEM.value(), 
                systemPrompt != null ? systemPrompt : "You are a helpful assistant."
            );
            ChatMessage userMessage = new ChatMessage(
                ChatMessageRole.USER.value(), 
                prompt
            );
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(actualModel)
                    .messages(List.of(systemMessage, userMessage))
                    .maxTokens(2000)
                    .temperature(0.1)
                    .build();

            var response = service.createChatCompletion(request);
            return response.getChoices().get(0).getMessage().getContent().trim();
        } catch (Exception e) {
            log.error("LLM chat failed with model: {}", actualModel, e);
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

Schema:
%s

Question: %s

Return ONLY the SQL query, no explanation. %s Always use LIMIT 1000 unless specified otherwise.
""".formatted(schemaContext, question, dialectHint);

        return chat(prompt, "You are a SQL expert. Convert natural language questions to database queries.", null);
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

    private OpenAiService createService() {
        String baseUrl = getBaseUrl();
        String apiKey = getApiKey();
        
        log.debug("Creating LLM service with baseUrl: {}, provider: {}", baseUrl, provider);
        
        // 使用正确的构造器
        return new OpenAiService(apiKey);
    }

    private String getBaseUrl() {
        return switch (provider) {
            case "deepseek" -> deepSeekBaseUrl;
            case "openai" -> localBaseUrl; // 可以配置为 OpenAI 兼容的代理
            default -> localBaseUrl; // 本地 Kimi/Doubao
        };
    }

    private String getApiKey() {
        return switch (provider) {
            case "deepseek" -> deepSeekApiKey;
            case "openai" -> localApiKey;
            default -> localApiKey;
        };
    }

    private String getDefaultModel() {
        return switch (defaultModel) {
            case "kimi" -> kimiModel;
            case "doubao" -> doubaoModel;
            case "deepseek" -> deepSeekModel;
            default -> defaultModel;
        };
    }

    public Map<String, String> getAvailableModels() {
        return Map.of(
            "kimi", kimiModel,
            "doubao", doubaoModel,
            "deepseek", deepSeekModel
        );
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setDefaultModel(String model) {
        this.defaultModel = model;
    }
}
