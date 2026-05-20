package com.openchat4u.chart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChartService {

    public ChartData generateChart(String question, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return createEmptyChart();
        }

        String chartType = detectChartType(question, data);
        
        return switch (chartType) {
            case "bar" -> generateBarChart(question, data);
            case "line" -> generateLineChart(question, data);
            case "pie" -> generatePieChart(question, data);
            case "table" -> generateTableChart(data);
            default -> generateTableChart(data);
        };
    }

    private String detectChartType(String question, List<Map<String, Object>> data) {
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.contains("趋势") || lowerQuestion.contains("变化") || lowerQuestion.contains("时间")) {
            return "line";
        }
        if (lowerQuestion.contains("占比") || lowerQuestion.contains("比例") || lowerQuestion.contains("分布")) {
            return "pie";
        }
        if (lowerQuestion.contains("对比") || lowerQuestion.contains("比较") || lowerQuestion.contains("排名")) {
            return "bar";
        }
        
        if (data.size() <= 10 && data.get(0).size() == 2) {
            return "pie";
        }
        if (data.size() > 10) {
            return "line";
        }
        
        return "bar";
    }

    private ChartData generateBarChart(String question, List<Map<String, Object>> data) {
        ChartData chart = new ChartData();
        chart.setChartType("bar");
        
        Map<String, Object> firstRow = data.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());
        
        if (columns.size() >= 2) {
            String labelColumn = columns.get(0);
            String valueColumn = columns.get(1);
            
            List<String> categories = data.stream()
                .map(row -> row.get(labelColumn).toString())
                .collect(Collectors.toList());
            
            List<Object> values = data.stream()
                .map(row -> {
                    Object val = row.get(valueColumn);
                    if (val instanceof Number) return val;
                    try {
                        return Double.parseDouble(val.toString());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
            
            chart.setCategories(categories);
            
            ChartData.Series series = new ChartData.Series();
            series.setName(valueColumn);
            series.setType("bar");
            series.setData(values);
            chart.setSeries(List.of(series));
        }
        
        return chart;
    }

    private ChartData generateLineChart(String question, List<Map<String, Object>> data) {
        ChartData chart = new ChartData();
        chart.setChartType("line");
        
        Map<String, Object> firstRow = data.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());
        
        if (columns.size() >= 2) {
            String labelColumn = columns.get(0);
            List<String> valueColumns = columns.subList(1, columns.size());
            
            List<String> categories = data.stream()
                .map(row -> row.get(labelColumn).toString())
                .collect(Collectors.toList());
            
            List<ChartData.Series> seriesList = new ArrayList<>();
            for (String valueColumn : valueColumns) {
                ChartData.Series series = new ChartData.Series();
                series.setName(valueColumn);
                series.setType("line");
                series.setData(data.stream()
                    .map(row -> {
                        Object val = row.get(valueColumn);
                        if (val instanceof Number) return val;
                        try {
                            return Double.parseDouble(val.toString());
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .collect(Collectors.toList()));
                seriesList.add(series);
            }
            
            chart.setCategories(categories);
            chart.setSeries(seriesList);
        }
        
        return chart;
    }

    private ChartData generatePieChart(String question, List<Map<String, Object>> data) {
        ChartData chart = new ChartData();
        chart.setChartType("pie");
        
        Map<String, Object> firstRow = data.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());
        
        if (columns.size() >= 2) {
            String labelColumn = columns.get(0);
            String valueColumn = columns.get(1);
            
            List<String> categories = data.stream()
                .map(row -> row.get(labelColumn).toString())
                .collect(Collectors.toList());
            
            List<Object> values = data.stream()
                .map(row -> {
                    Object val = row.get(valueColumn);
                    if (val instanceof Number) return val;
                    try {
                        return Double.parseDouble(val.toString());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
            
            chart.setCategories(categories);
            
            ChartData.Series series = new ChartData.Series();
            series.setName(valueColumn);
            series.setType("pie");
            series.setData(values);
            chart.setSeries(List.of(series));
        }
        
        return chart;
    }

    private ChartData generateTableChart(List<Map<String, Object>> data) {
        ChartData chart = new ChartData();
        chart.setChartType("table");
        chart.setCategories(new ArrayList<>(data.get(0).keySet()));
        
        ChartData.Series series = new ChartData.Series();
        series.setName("data");
        series.setType("table");
        series.setData(data.stream()
            .map(row -> new ArrayList<>(row.values()))
            .collect(Collectors.toList()));
        chart.setSeries(List.of(series));
        
        return chart;
    }

    private ChartData createEmptyChart() {
        ChartData chart = new ChartData();
        chart.setChartType("table");
        chart.setCategories(new ArrayList<>());
        chart.setSeries(new ArrayList<>());
        return chart;
    }
}
