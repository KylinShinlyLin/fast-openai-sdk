package com.kylin.fast.openai.request.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AudioMessageContent {

    /**
     * 内容类型 text/input_audio
     */
    String type;

    /**
     * 与文本聊天的内容
     */
    String text;


    /**
     * 音频数据
     */
    InputAudio input_audio;


    @EqualsAndHashCode
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InputAudio {

        /**
         * bas64 数据
         */
        String data;

        /**
         * 数据类型
         */
        String format;

    }
}
