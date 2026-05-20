package com.openchat4u.query;

import com.openchat4u.chart.ChartData;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    private String answer;
    private String sql;
    private List<Map<String, Object>> data;
    private String error;
    private ChartData chart;
    private String conversationId;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public ChartData getChart() { return chart; }
    public void setChart(ChartData chart) { this.chart = chart; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public static QueryResponse success(String answer, String sql, List<Map<String, Object>> data) {
        QueryResponse resp = new QueryResponse();
        resp.setAnswer(answer);
        resp.setSql(sql);
        resp.setData(data);
        return resp;
    }

    public static QueryResponse error(String error) {
        QueryResponse resp = new QueryResponse();
        resp.setError(error);
        return resp;
    }
}
