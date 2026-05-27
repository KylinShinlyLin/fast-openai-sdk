package com.kylin.fast.graph.core;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GraphConfig {
    private String threadId;
    private List<String> interruptBefore;
    private List<String> interruptAfter;
}
