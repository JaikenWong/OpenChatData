package com.openchat4u.schema;

import lombok.Data;
import java.util.List;

@Data
public class TableSchema {
    private String tableName;
    private String tableComment;
    private List<ColumnSchema> columns;

    @Data
    public static class ColumnSchema {
        private String columnName;
        private String dataType;
        private String columnComment;
        private Boolean nullable;
    }
}
