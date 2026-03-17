package com.kylin.fast.openai.request;

import com.google.common.collect.Lists;
import com.kylin.fast.openai.constant.MessageType;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.request.dto.AudioMessageContent;
import com.kylin.fast.openai.request.dto.BaseMessage;
import com.kylin.fast.openai.utils.Base64Tools;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.util.List;

@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AudioMessage implements BaseMessage {

    /**
     * MessageRole
     */
    String role;

    List<AudioMessageContent> content;

    @Override
    public String roleType() {
        return role;
    }

    @Override
    public MessageType messageType() {
        return MessageType.AUDIO;
    }


    public static AudioMessage of(MessageRole role, File audio, String format) {
        return of(role, Base64Tools.encodeBase64(audio), format);
    }


    public static AudioMessage of(MessageRole role, String base64, String format) {
        return AudioMessage.builder()
                .role(role.role)
                .content(Lists.newArrayList(
                        AudioMessageContent.builder()
                                .type("input_audio")
                                .input_audio(AudioMessageContent.InputAudio.builder()
                                        .data(base64)
                                        .format(format)
                                        .build())
                                .build()))
                .build();
    }

}
