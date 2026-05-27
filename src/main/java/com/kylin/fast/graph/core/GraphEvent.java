package com.kylin.fast.graph.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphEvent {
    private String type; // "node_start", "node_end", "token", "end_of_stream"
    private String nodeName;
    private Object payload; // 对于 node_end 是状态快照/更新，对于 token 是增量文本
}
