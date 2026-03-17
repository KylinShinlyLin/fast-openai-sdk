package com.kylin.fast.openai.constant;

import lombok.AllArgsConstructor;

/**
 * Created by ZengShiLin on 2023/10/24 10:11
 */
@AllArgsConstructor
public enum WhisperLanguage {

    /**
     * 语言枚举
     */
    CHINESE("zh"),
    ENGLISH("en"),
    GERMAN("de"),
    ARABIC("ar"),
    FRENCH("fr"),
    RUSSIAN("ru"),
    ITALIAN("it"),
    SPANISH("es"),
    MALAY("ms"),
    VIETNAMESE("vi"),
    JAPANESE("ja"),
    KOREAN("ko"),
    THAI("th"),
    INDONESIAN("id"),
    PORTUGUESE("pt");

    public final String code;

}
