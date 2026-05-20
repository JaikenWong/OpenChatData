package com.openchat4u.query;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class QueryRequest {
    private String question;
    private List<String> tables;
    private String conversationId;
}
