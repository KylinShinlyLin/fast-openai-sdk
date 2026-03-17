package com.kylin.fast.openai.request;

import lombok.*;

/**
 * Created by ZengShilin on 2023/3/2 7:52 PM
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class AudioTextRequest {


    String  model;

    /**
     * An optional text to guide the model's style or continue a previous audio segment. The prompt should match the audio language.
     */
    String prompt;

    /**
     * The format of the transcript output, in one of these options: json, text, srt, verbose_json, or vtt.
     */
    String responseFormat;

    /**
     * 和 gpt temperature 一样性质
     */
    Double temperature;

    /**
     * 定向识别语言，不填默认通过自动推断识别语言类型
     * 参考 ISO-639-1 编码格式 https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes
     */
    String language;
}
