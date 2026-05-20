package com.openchat4u.chart;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChartData {
    private String chartType;
    private List<String> categories;
    private List<Series> series;
    private Map<String, Object> options;

    @Data
    public static class Series {
        private String name;
        private List<Object> data;
        private String type;
    }
}
