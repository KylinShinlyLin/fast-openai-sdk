package com.kylin.fast.graph.model;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public class ChatMessageTest {
    @Test
    public void testSerialization() {
        ToolCall tc = new ToolCall();
        tc.setId("call_01");
        tc.setName("calculator");
        tc.setArguments("{\"a\":1}");

        ChatMessage msg = ChatMessage.assistant("thinking", Collections.singletonList(tc));
        String json = JSON.toJSONString(msg);

        ChatMessage deserialized = JSON.parseObject(json, ChatMessage.class);
        assertEquals("assistant", deserialized.getRole());
        assertEquals("thinking", deserialized.getContent());
        assertNotNull(deserialized.getToolCalls());
        assertEquals(1, deserialized.getToolCalls().size());
        assertEquals("calculator", deserialized.getToolCalls().get(0).getName());
    }
}
