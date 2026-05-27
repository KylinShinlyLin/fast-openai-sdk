package com.kylin.fast.graph.provider;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class ModelOptions {
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private List<ToolDefinition> tools;
}
