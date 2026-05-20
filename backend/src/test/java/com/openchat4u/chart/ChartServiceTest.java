package com.openchat4u.chart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

class ChartServiceTest {

    private ChartService chartService;

    @BeforeEach
    void setUp() {
        chartService = new ChartService();
    }

    @Test
    void testGenerateBarChart() {
        String question = "对比各部门销售额";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("销售部", 100000),
            createRow("市场部", 80000),
            createRow("技术部", 120000)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("bar", result.getChartType());
        assertEquals(3, result.getCategories().size());
        assertEquals("销售部", result.getCategories().get(0));
        assertEquals(1, result.getSeries().size());
        assertEquals("amount", result.getSeries().get(0).getName());
    }

    @Test
    void testGenerateLineChart() {
        String question = "查看销售额变化趋势";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("2024-01", 100000),
            createRow("2024-02", 120000),
            createRow("2024-03", 110000),
            createRow("2024-04", 130000),
            createRow("2024-05", 140000),
            createRow("2024-06", 150000),
            createRow("2024-07", 160000),
            createRow("2024-08", 170000),
            createRow("2024-09", 180000),
            createRow("2024-10", 190000),
            createRow("2024-11", 200000)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("line", result.getChartType());
        assertEquals(11, result.getCategories().size());
    }

    @Test
    void testGeneratePieChart() {
        String question = "查看各部门占比";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("销售部", 40),
            createRow("市场部", 30),
            createRow("技术部", 30)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("pie", result.getChartType());
        assertEquals(3, result.getCategories().size());
    }

    @Test
    void testGeneratePieChartSmallData() {
        String question = "查询数据";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("A", 10),
            createRow("B", 20),
            createRow("C", 30)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("pie", result.getChartType());
    }

    @Test
    void testGenerateTableChart() {
        String question = "查询所有数据";
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i);
            row.put("name", "Item " + i);
            row.put("value", i * 100);
            data.add(row);
        }

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("line", result.getChartType());
    }

    @Test
    void testGenerateChartWithEmptyData() {
        String question = "查询数据";
        List<Map<String, Object>> data = new ArrayList<>();

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("table", result.getChartType());
        assertTrue(result.getCategories().isEmpty());
    }

    @Test
    void testGenerateChartWithNullData() {
        String question = "查询数据";

        ChartData result = chartService.generateChart(question, null);

        assertNotNull(result);
        assertEquals("table", result.getChartType());
    }

    @Test
    void testDetectChartTypeByKeywords() {
        List<Map<String, Object>> data = Arrays.asList(
            createRow("A", 10),
            createRow("B", 20)
        );

        assertEquals("line", chartService.generateChart("趋势分析", data).getChartType());
        assertEquals("line", chartService.generateChart("时间变化", data).getChartType());
        assertEquals("pie", chartService.generateChart("占比分析", data).getChartType());
        assertEquals("pie", chartService.generateChart("比例分布", data).getChartType());
        assertEquals("bar", chartService.generateChart("对比分析", data).getChartType());
        assertEquals("bar", chartService.generateChart("排名情况", data).getChartType());
    }

    @Test
    void testBarChartDataConversion() {
        String question = "对比销售额";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("A", 100.5),
            createRow("B", 200.3),
            createRow("C", 300.7)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals(3, result.getSeries().get(0).getData().size());
        assertEquals(100.5, result.getSeries().get(0).getData().get(0));
    }

    @Test
    void testBarChartWithStringValues() {
        String question = "对比销售额";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("A", "100"),
            createRow("B", "200"),
            createRow("C", "invalid")
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals(100.0, result.getSeries().get(0).getData().get(0));
        assertEquals(200.0, result.getSeries().get(0).getData().get(1));
        assertEquals(0, result.getSeries().get(0).getData().get(2));
    }

    @Test
    void testLineChartMultipleSeries() {
        String question = "查看趋势";
        List<Map<String, Object>> data = Arrays.asList(
            createMultiRow("2024-01", 100, 200),
            createMultiRow("2024-02", 150, 250),
            createMultiRow("2024-03", 120, 220)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result);
        assertEquals("line", result.getChartType());
        assertEquals(3, result.getCategories().size());
        assertEquals(2, result.getSeries().size());
    }

    @Test
    void testChartDataStructure() {
        String question = "对比数据";
        List<Map<String, Object>> data = Arrays.asList(
            createRow("A", 10),
            createRow("B", 20)
        );

        ChartData result = chartService.generateChart(question, data);

        assertNotNull(result.getChartType());
        assertNotNull(result.getCategories());
        assertNotNull(result.getSeries());
        assertFalse(result.getSeries().isEmpty());
        assertNotNull(result.getSeries().get(0).getName());
        assertNotNull(result.getSeries().get(0).getData());
    }

    private Map<String, Object> createRow(String label, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", label);
        row.put("amount", value);
        return row;
    }

    private Map<String, Object> createMultiRow(String label, Object value1, Object value2) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", label);
        row.put("value1", value1);
        row.put("value2", value2);
        return row;
    }
}
