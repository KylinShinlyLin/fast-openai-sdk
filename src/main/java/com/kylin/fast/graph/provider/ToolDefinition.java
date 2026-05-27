package com.kylin.fast.graph.provider;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ToolDefinition {
    private String name;
    private String description;
    private String parametersSchema;
}
