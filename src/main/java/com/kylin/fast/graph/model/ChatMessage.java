package com.kylin.fast.graph.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.File;
import com.kylin.fast.openai.utils.Base64Tools;
import com.kylin.fast.openai.request.dto.BaseMessage;
import com.kylin.fast.openai.request.dto.ImgMessage;
import com.kylin.fast.openai.request.dto.ImgMessageContent;
import com.kylin.fast.openai.request.dto.ImageUrl;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.request.dto.GptFunction;
import com.kylin.fast.openai.request.dto.GptTool;
import java.util.ArrayList;
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

    /**
     * 从 SDK BaseMessage 转换为 ChatMessage（Redis 持久化 → Graph 状态）。
     */
    public static ChatMessage from(BaseMessage bm) {
        if (bm instanceof ImgMessage) {
            ImgMessage img = (ImgMessage) bm;
            List<String> urls = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            if (img.getContent() != null) {
                for (ImgMessageContent ic : img.getContent()) {
                    if (ic.getImage_url() != null && ic.getImage_url().getUrl() != null) {
                        urls.add(ic.getImage_url().getUrl());
                    } else if ("text".equals(ic.getType()) && ic.getText() != null) {
                        text.append(ic.getText());
                    }
                }
            }
            return ChatMessage.image(text.toString(), urls);
        }
        if (bm instanceof Message) {
            Message msg = (Message) bm;
            if ("tool".equals(msg.roleType())) {
                return ChatMessage.tool(msg.getName(), msg.getContent(), msg.getToolCallId());
            }
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                List<ToolCall> tcs = new ArrayList<>();
                for (GptTool gt : msg.getToolCalls()) {
                    if (gt.getFunction() != null) {
                        tcs.add(new ToolCall(gt.getId(), gt.getFunction().getName(), gt.getFunction().getArguments()));
                    }
                }
                return ChatMessage.assistant(msg.getContent(), tcs);
            }
            if ("assistant".equals(msg.roleType())) {
                return ChatMessage.assistant(msg.getContent());
            }
            if ("system".equals(msg.roleType())) {
                return ChatMessage.system(msg.getContent());
            }
            return ChatMessage.user(msg.getContent());
        }
        return ChatMessage.user(bm.toString());
    }

    /**
     * 从 ChatMessage 转换为 SDK BaseMessage（Graph 状态 → Redis 持久化 / SDK 调用）。
     */
    public BaseMessage toBaseMessage() {
        if ("image".equals(this.messageType) && this.imageUrls != null && !this.imageUrls.isEmpty()) {
            List<ImgMessageContent> contents = new ArrayList<>();
            if (this.content != null && !this.content.isEmpty()) {
                contents.add(ImgMessageContent.builder().type("text").text(this.content).build());
            }
            for (String url : this.imageUrls) {
                contents.add(ImgMessageContent.builder().type("image_url")
                        .image_url(ImageUrl.builder().url(url).detail("high").build()).build());
            }
            return ImgMessage.builder().role(this.role).content(contents).build();
        }
        Message msg = new Message();
        msg.setRole(this.role);
        msg.setContent(this.content);
        msg.setName(this.name);
        msg.setToolCallId(this.toolCallId);
        if (this.toolCalls != null && !this.toolCalls.isEmpty()) {
            List<GptTool> tcs = new ArrayList<>();
            for (ToolCall tc : this.toolCalls) {
                tcs.add(GptTool.builder().id(tc.getId())
                        .function(GptFunction.builder().name(tc.getName()).arguments(tc.getArguments()).build())
                        .type("function").build());
            }
            msg.setToolCalls(tcs);
        }
        return msg;
    }
}
