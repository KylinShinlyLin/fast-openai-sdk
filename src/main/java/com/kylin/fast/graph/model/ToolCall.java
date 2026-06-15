package com.kylin.fast.graph.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ToolCall {
    private String id;
    private String name;
    private String arguments;
    // 🌟 Gemini thought_signature: functionCall Part 要求携带，用于 Gemini 2.5 Flash 工具调用往返
    private byte[] thoughtSignature;

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public ToolCall(String id, String name, String arguments, byte[] thoughtSignature) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
        this.thoughtSignature = thoughtSignature;
    }
}
