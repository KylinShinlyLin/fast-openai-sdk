package com.kylin.fast.graph.model;

import lombok.Data;
import java.util.List;

@Data
public class ChatMessageChunk {
    private String contentChunk;
    private List<ToolCallChunk> toolCallChunks;
}
