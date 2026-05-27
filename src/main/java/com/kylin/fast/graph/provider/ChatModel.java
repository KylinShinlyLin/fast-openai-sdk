package com.kylin.fast.graph.provider;

import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.graph.model.ChatMessageChunk;
import java.util.Iterator;
import java.util.List;

public interface ChatModel {
    ChatMessage chat(List<ChatMessage> messages, ModelOptions options);
    Iterator<ChatMessageChunk> streamChat(List<ChatMessage> messages, ModelOptions options);
}
