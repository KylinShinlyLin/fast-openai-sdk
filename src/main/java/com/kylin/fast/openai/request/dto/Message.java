package com.kylin.fast.openai.request.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.annotations.SerializedName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylin.fast.openai.constant.MessageType;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.result.Audio;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Created by ZengShilin on 2023/3/2 10:52 AM
 *
 * @author ZengShilin
 */
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Message implements BaseMessage {

    /**
     * MessageRole
     */
    public String role;

    String content;


    @SerializedName("reasoning_content")
    String reasoningContent;

    String name;

    @SerializedName("tool_calls")
    List<GptTool> toolCalls;


    @SerializedName("tool_call_id")
    String toolCallId;


    String refusal;


    Audio audio;

    @SerializedName("thinking_blocks")
    JSONArray thinking_blocks;


    @Override
    public String roleType() {
        return role;
    }

    @Override
    public MessageType messageType() {
        return MessageType.TEXT;
    }

    public static Message of(MessageRole role, String content) {
        return Message.builder()
                .role(role.role)
                .content(content)
                .build();
    }

}
