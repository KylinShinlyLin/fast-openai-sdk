package com.kylin.fast.openai.function;

import com.kylin.fast.openai.request.dto.Message;

/**
 * 函数调用上下文处理器
 * 用于在自动函数调用循环中，将产生的中间消息（Assistant消息和Tool消息）同步保存到外部存储（如Redis/DB）
 */
public interface FunctionContextHandler {

    /**
     * 当 AI 决定调用函数时触发（保存 Assistant 的 ToolCall 消息）
     *
     * @param message 包含 tool_calls 的 Assistant 消息
     */
    void onAssistantMessage(Message message);

    /**
     * 当 函数执行完毕获得结果时触发（保存 Tool 消息）
     *
     * @param message 包含 function result 的 Tool 消息
     */
    void onToolMessage(Message message);
}
