package com.kylin.fast.graph.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.File;
import com.kylin.fast.openai.utils.Base64Tools;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role; // "system", "user", "assistant", "tool"
    private String content;
    private String name;
    private List<ToolCall> toolCalls;
    private String toolCallId;

    // 多模态支持：存储图片 URL (可以是网络链接 http 或者是本地 Base64)
    private List<String> imageUrls;
    private String messageType = "text"; // "text", "image"

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null, null, null, "text");
    }
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null, null, null, "text");
    }
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null, null, null, "text");
    }
    public static ChatMessage assistant(String content, List<ToolCall> toolCalls) {
        return new ChatMessage("assistant", content, null, toolCalls, null, null, "text");
    }
    public static ChatMessage tool(String name, String content) {
        return new ChatMessage("tool", content, name, null, null, null, "text");
    }
    public static ChatMessage tool(String name, String content, String toolCallId) {
        return new ChatMessage("tool", content, name, null, toolCallId, null, "text");
    }

    // 🌟 多模态：一键创建携带多张图片的图片消息类型！
    public static ChatMessage image(String content, List<String> imageUrls) {
        ChatMessage msg = new ChatMessage("user", content, null, null, null, imageUrls, "image");
        return msg;
    }

    // 🌟 多模态：一键创建携带单张图片的图片消息类型！
    public static ChatMessage image(String content, String imageUrl) {
        return new ChatMessage("user", content, null, null, null, Collections.singletonList(imageUrl), "image");
    }

    // 🌟 多模态：一键创建直接携带本地 File 图片的图片消息类型！
    public static ChatMessage image(String content, File imageFile) {
        String base64 = "data:image/png;base64," + Base64Tools.encodeBase64(imageFile);
        return image(content, base64);
    }

    // 🌟 多模态：一键创建直接携带多个本地 File 图片的可变参数方法，规避泛型擦除冲突！
    public static ChatMessage image(String content, File... imageFiles) {
        List<String> base64List = new java.util.ArrayList<>();
        if (imageFiles != null) {
            for (File f : imageFiles) {
                base64List.add("data:image/png;base64," + Base64Tools.encodeBase64(f));
            }
        }
        return image(content, base64List);
    }
}
