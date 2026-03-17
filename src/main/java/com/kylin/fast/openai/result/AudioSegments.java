package com.kylin.fast.openai.result;

import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * Created by ZengShiLin on 2023/7/21 9:57 AM
 */
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AudioSegments {

    String id;

    String seek;

    String start;

    String end;

    String text;

    int[] tokens;

    Double temperature;

    /**
     * 序列可能性 生成文本的平均对数概率。
     * 负数代表可能性很低
     */
    Double avg_logprob;

    /**
     * 压缩比率，即生成文本相对于原始文本的压缩程度。
     */
    Double compression_ratio;

    /**
     * 分段中没有语音的概率
     * 大于 0.7 基本可以判定为空内容
     */
    Double no_speech_prob;
}
