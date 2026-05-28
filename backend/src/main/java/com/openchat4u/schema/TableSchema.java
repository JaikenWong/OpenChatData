package com.openchat4u.schema;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TableSchema {
    private String tableName;
    private String tableComment;
    private List<ColumnSchema> columns;
    private List<Map<String, Object>> sampleRows;

    @Data
    public static class ColumnSchema {
        private String columnName;
        private String dataType;
        private String columnComment;
        private Boolean nullable;
    }
}
