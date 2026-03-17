package com.kylin.fast.openai.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum WhisperAsrLanguage {
    /**
     * 中文-简体中文
     */
    CHINESE("chinese"),
    /**
     * 粤语
     */
    CANTONESE(Arrays.asList("chinese", "cantonese")),
    /**
     * 英语
     */
    ENGLISH("english"),
    /**
     * 阿拉伯语
     */
    ARABIC("arabic"),
    /**
     * 印度尼西亚语
     */
    INDONESIAN("indonesian"),
    /**
     * 日本语
     */
    JAPANESE("japanese"),
    /**
     * 西班牙语
     */
    SPANISH("spanish"),
    /**
     * 葡萄牙语
     */
    PORTUGUESE("portuguese"),
    /**
     * 越南语
     */
    VIETNAMESE("vietnamese"),
    /**
     * 德语
     */
    GERMAN("german"),
    /**
     * 意大利语
     */
    ITALIAN("italian"),
    /**
     * 俄语
     */
    RUSSIAN("russian"),
    /**
     * 韩语
     */
    KOREAN("korean"),
    /**
     * 泰语
     */
    THAI("thai"),
    /**
     * 法语
     */
    FRENCH("french"),
    /**
     * 马来语
     */
    MALAY("malay"),
    /**
     * 波兰语
     */
    POLISH("polish"),
    /**
     * 乌克兰语
     */
    UKRAINIAN("ukrainian"),
    /**
     * 拉脱维亚语
     */
    LATVIAN("latvian"),
    /**
     * 立陶宛语
     */
    LITHUANIAN("lithuanian"),
    /**
     * 保加利亚语
     */
    BULGARIAN("bulgarian"),
    /**
     * 土耳其语
     */
    TURKISH("turkish"),
    /**
     * 印地语
     */
    HINDI("hindi"),
    /**
     * 缅甸语
     */
    MYANMAR("myanmar"),
    /**
     * 孟加拉语
     */
    BENGALI("bengali"),
    /**
     * 老挝语
     */
    LAO("lao"),

    /**
     * 菲律宾语（塔加拉格语）
     */
    TAGALOG("tagalog"),

    /**
     * 菲律宾语
     */
    FILIPINO("tagalog"),
    ;


    WhisperAsrLanguage(String vendorResponseLanguageCode) {
        this.vendorResponseLanguageCodeList = Arrays.asList(vendorResponseLanguageCode);
    }

    private final List<String> vendorResponseLanguageCodeList;


}
