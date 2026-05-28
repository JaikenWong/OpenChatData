package com.openchat4u.history;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("query_history")
public class QueryHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;
    private String question;
    private String sql;
    private String answer;
    private Integer resultCount;
    private Long executionTimeMs;
    private Boolean isSuccess;
    private String errorMessage;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
