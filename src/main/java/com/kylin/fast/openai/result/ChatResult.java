package com.kylin.fast.openai.result;

import com.alibaba.fastjson.JSON;
import com.google.gson.annotations.SerializedName;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.result.dto.ChatChoice;
import com.kylin.fast.openai.result.dto.Usage;
import com.kylin.fast.openai.utils.GptResultUtil;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An object containing a response from the completion api
 * <p>
 * https://beta.openai.com/docs/api-reference/completions/create
 *
 * @author zengshilin
 */
@Data
@ToString
public class ChatResult {
    /**
     * A unique id assigned to this completion.
     */
    String id;

    /**
     * The type of object returned, should be "text_completion"
     */
    String object;


    /**
     * The creation time in epoch seconds.
     */
    long created;

    /**
     * The GPT-3 model used.
     */
    String model;

    /**
     * A list of generated completions.
     */
    List<ChatChoice> choices;

    /**
     * The API usage for this request
     */
    Usage usage;

    @SerializedName("system_fingerprint")
    String system_fingerprint;


    /**
     * 判断是否触发函数
     *
     * @return true/false
     */
    public boolean triggerFunction() {
        return this.getChoices().stream()
                .map(ChatChoice::getMessage)
                .filter(Objects::nonNull)
                .map(Message::getToolCalls)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .anyMatch(e -> Objects.nonNull(e.getFunction()));
    }


    public String content() {
        if (CollectionUtils.isEmpty(choices)) {
            return null;
        }

        if (Objects.nonNull(choices.get(0).getMessage().getAudio())) {
            return choices.get(0).getMessage().getAudio().getTranscript();
        }

        return choices
                .stream()
                .map(ChatChoice::getMessage)
                .map(Message::getContent)
                //过滤掉独立的换行符
                .filter(e -> !"\n".equals(e))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining()).trim();
    }

    public File audio() {
        if (CollectionUtils.isEmpty(choices)) {
            return null;
        }

        if (Objects.nonNull(choices.get(0).getMessage().getAudio())) {
            return choices.get(0).getMessage().getAudio().audioFile();
        } else {
            return null;
        }
    }


    /**
     * response schema 格式下解析 结构
     *
     * @param clazz Class<T>
     * @param <T>   指定类型
     * @return obj
     */
    public <T> T parseObj(Class<T> clazz) {
        return JSON.parseObject(GptResultUtil.response(this), clazz);
    }

    public String parseText() {
        return GptResultUtil.response(this);
    }
}
