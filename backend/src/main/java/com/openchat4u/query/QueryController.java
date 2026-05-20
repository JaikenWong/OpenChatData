package com.openchat4u.query;

import com.openchat4u.auth.JwtTokenProvider;
import com.openchat4u.chart.ChartService;
import com.openchat4u.dictionary.DictionaryService;
import com.openchat4u.history.QueryHistory;
import com.openchat4u.history.QueryHistoryService;
import com.openchat4u.masking.DataMaskingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final QueryService queryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final DictionaryService dictionaryService;
    private final QueryHistoryService historyService;
    private final DataMaskingService maskingService;
    private final ChartService chartService;

    @PostMapping("/ask")
    public QueryResponse ask(@RequestHeader("Authorization") String authorization,
                             @RequestBody QueryRequest request) {
        String token = authorization.substring(7);
        String tenantCode = jwtTokenProvider.getTenantCode(token);
        Long userId = jwtTokenProvider.getUserId(token);

        long startTime = System.currentTimeMillis();
        QueryHistory history = new QueryHistory();
        history.setTenantCode(tenantCode);
        history.setUserId(userId);
        history.setQuestion(request.getQuestion());

        try {
            String enhancedQuestion = dictionaryService.enhanceQuestion(tenantCode, request.getQuestion());
            request.setQuestion(enhancedQuestion);

            QueryResponse response = queryService.ask(tenantCode, request);

            long executionTime = System.currentTimeMillis() - startTime;
            history.setSql(response.getSql());
            history.setAnswer(response.getAnswer());
            history.setIsSuccess(response.getError() == null);
            history.setExecutionTimeMs(executionTime);

            if (response.getData() != null) {
                history.setResultCount(response.getData().size());
                List<Map<String, Object>> maskedData = response.getData();
                if (request.getTables() != null && !request.getTables().isEmpty()) {
                    maskedData = maskingService.applyMasking(tenantCode, request.getTables().get(0), response.getData());
                    response.setData(maskedData);
                }
                
                response.setChart(chartService.generateChart(enhancedQuestion, maskedData));
            }

            if (response.getError() != null) {
                history.setErrorMessage(response.getError());
            }

            historyService.save(history);
            
            response.setConversationId("default");
            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            history.setIsSuccess(false);
            history.setErrorMessage(e.getMessage());
            history.setExecutionTimeMs(executionTime);
            historyService.save(history);
            log.error("Query failed", e);
            return QueryResponse.error("Query failed: " + e.getMessage());
        }
    }
}
