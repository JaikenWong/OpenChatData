package com.openchat4u.schema;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;

    @GetMapping("/{tenantCode}/tables")
    public List<String> getTables(@PathVariable String tenantCode) {
        return schemaService.getTables(tenantCode);
    }

    @GetMapping("/{tenantCode}/tables/{tableName}")
    public TableSchema getTableSchema(@PathVariable String tenantCode, @PathVariable String tableName) {
        return schemaService.getTableSchema(tenantCode, tableName);
    }

    @PostMapping("/{tenantCode}/sync")
    public Map<String, Object> syncSchema(@PathVariable String tenantCode, @RequestBody List<String> tableNames) {
        schemaService.syncSchemaToQdrant(tenantCode, tableNames);
        return Map.of("status", "success", "synced", tableNames.size());
    }
}
