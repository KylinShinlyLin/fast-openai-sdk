package com.kylin.fast.openai.result;

import com.kylin.fast.openai.request.dto.GptFunction;
import com.kylin.fast.openai.request.dto.GptTool;
import com.kylin.fast.openai.result.dto.ChatCompletionStreamChoice;
import com.kylin.fast.openai.result.dto.ChatMessage;
import com.kylin.fast.openai.result.dto.Usage;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An object containing a response from the completion api
 * <p>
 * https://beta.openai.com/docs/api-reference/completions/create
 */
@Data
@ToString
public class ChatStreamResult {
    /**
     * Unique id assigned to this chat completion.
     */
    String id;

    /**
     * The type of object returned, should be "chat.completion.chunk"
     */
    String object;

    /**
     * The creation time in epoch seconds.
     */
    long created;

    /**
     * The model used.
     */
    String model;

    /**
     * A list of all generated completions.
     */
    List<ChatCompletionStreamChoice> choices;


    String system_fingerprint;

    /**
     * 流式完成后才有结果
     */
    Usage usage;


    /**
     * 判断是否触发函数
     *
     * @return 是否
     */
    public boolean triggerFunction() {
        return Optional.of(this)
                .map(ChatStreamResult::getChoices)
                .filter(CollectionUtils::isNotEmpty)
                .map(e -> e.get(0))
                .map(ChatCompletionStreamChoice::getDelta)
                .filter(e -> !CollectionUtils.isEmpty(e.getToolCalls()))
                .isPresent();
    }

    public String getFunctionName() {
        return Optional.of(this)
                .map(ChatStreamResult::getChoices)
                .filter(CollectionUtils::isNotEmpty)
                .map(e -> e.get(0))
                .map(ChatCompletionStreamChoice::getDelta)
                .map(ChatMessage::getToolCalls)
                .map(e -> e.get(0))
                .map(GptTool::getFunction)
                .map(GptFunction::getName)
                .orElse("");
    }

    public String text() {
        return Optional.of(this)
                .map(ChatStreamResult::getChoices)
                .filter(CollectionUtils::isNotEmpty)
                .map(e -> e.get(0))
                .map(ChatCompletionStreamChoice::getDelta)
                .map(ChatMessage::getContent)
                .orElse("");
    }

    public String functionArguments() {
        return Optional.of(this)
                .map(ChatStreamResult::getChoices)
                .filter(CollectionUtils::isNotEmpty)
                .map(e -> e.get(0))
                .map(ChatCompletionStreamChoice::getDelta)
                .map(ChatMessage::getToolCalls)
                .map(e -> e.get(0))
                .map(GptTool::getFunction)
                .map(GptFunction::getArguments)
                .orElse("");
    }
}
