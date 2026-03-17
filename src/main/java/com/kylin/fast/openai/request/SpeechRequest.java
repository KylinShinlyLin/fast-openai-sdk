package com.kylin.fast.openai.request;

import lombok.*;

/**
 * Created by ZengShiLin on 2023/11/7 09:32
 *
 * @author zengming
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class SpeechRequest {

    /**
     * 模型 可用的 TTS 型号之一： tts-1 或 tts-1-hd
     */
    String model;

    /**
     * 要为其生成音频的文本。最大长度为 4096 个字符
     */
    String input;

    /**
     * 生成音频时要使用的语音。支持的语音包括 alloy 、 、 、 echo fable onyx 和 nova shimmer
     */
    String voice;

    /**
     * The format to audio in. Supported formats are mp3, opus, aac, and flac.
     */
    String response_format;

    /**
     * 音频的速度。从 0.25 到 4.0 中选择一个值。 1.0 是默认值。
     */
    Double speed;
}
