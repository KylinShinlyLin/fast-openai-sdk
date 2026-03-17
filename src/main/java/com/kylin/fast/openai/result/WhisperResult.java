package com.kylin.fast.openai.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文生图入参
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/images/create">...</a>
 * Created by ZengShilin on 2023/3/2 5:24 PM
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WhisperResult {

    /**
     * 任务类型 transcribe 为转录任务 translators 为翻译任务
     */
    String task;

    /**
     * 推测出的语言类型
     */
    String language;

    /**
     * 语音时长
     */
    Double duration;

    /**
     * 完整文本
     */
    String text;

    List<AudioSegments> segments;


    /**
     * 过滤掉空语音
     *
     * @return
     */
    public String filterNoSpeechResult() {
        if (CollectionUtils.isEmpty(segments)) {
            return StringUtils.EMPTY;
        }
        return segments.stream()
                .filter(e -> e.getNo_speech_prob() < 0.65)
                .map(AudioSegments::getText)
                .collect(Collectors.joining());
    }

    /**
     * 当使用 verbose_json 的时候，会根据返回的参数，过滤掉可信度低的内容
     * 非 verbose_json 模式的时候 segments 为空，返回会为空
     *
     * @return
     */
    public String verboseJsonResult(boolean need_avg_logprob) {
        if (CollectionUtils.isEmpty(segments)) {
            return StringUtils.EMPTY;
        }
        return segments.stream()
                //根据空语音，和平局对数概率过滤掉不可信的内容
                //过滤掉空语音

                .filter(e -> {
                    Double logprob = e.getAvg_logprob();
                    Double noSpeechProb = e.getNo_speech_prob();
                    //noSpeech 概率高，但是相关性也高 很可能说的只是一串数字应该返回
                    if (noSpeechProb > 0.7 && logprob > -0.55) {
                        return true;
                    }
                    //超过 0.7 很大的可能性是空语音
                    boolean noSpeech = noSpeechProb > 0.7;
                    //空语音可能性不高的情况下， 对数概率很低，转录的结果可信度不高
                    if (need_avg_logprob) {
                        return logprob > -0.75 && !noSpeech;
                    }
                    // need_avg_logprob 为false 的情况下，只过滤空语音
                    return !noSpeech;
                })
                .map(AudioSegments::getText)
                .collect(Collectors.joining());
    }

}
