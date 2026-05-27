package com.kylin.fast.graph.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {
    private String threadId;
    private String checkpointId;
    private Map<String, Object> stateSnapshot;
    private String nextNode;
    private long createdAt;
}
