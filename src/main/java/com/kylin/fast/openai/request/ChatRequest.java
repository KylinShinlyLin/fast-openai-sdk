package com.kylin.fast.openai.request;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import com.kylin.fast.openai.request.dto.*;
import com.kylin.fast.openai.request.dto.BaseMessage;
import com.kylin.fast.openai.request.dto.GptTool;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by ZengShilin on 2023/3/2 10:43 AM
 *
 * @author ZengShilin
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class ChatRequest {

    /**
     *
     */
    String model;

    @SerializedName("reasoning_effort")
    String reasoningEffort;
    /**
     * 返回消息类型 ["text", "audio"]
     */
    List<String> modalities;

    /**
     * 声音设置
     */
    AudioSetting audio;

    /**
     * 消息内容
     */
    List<BaseMessage> messages;

    /**
     * Controls which (if any) function is called by the model. none means the model will not call a function and instead generates a message. auto means the model can pick between generating a message or calling a function. Specifying a particular function via {"type: "function", "function": {"name": "my_function"}} forces the model to call that function.
     * <p>
     * none is the default when no functions are present. auto is the default if functions are present.
     * auto 表示自动识别
     */
    @SerializedName("tool_choice")
    String toolChoice;

    /**
     * A list of tools the model may call. Currently, only functions are supported as a tool. Use this to provide a list of functions the model may generate JSON inputs for.
     */
    List<GptTool> tools;

    @SerializedName("parallel_tool_calls")
    Boolean parallelToolCalls;

    /**
     * What sampling temperature to use. Higher values means the model will take more risks.
     * Try 0.9 for more creative applications, and 0 (argmax sampling) for ones with a well-defined answer.
     * We generally recommend using this or {@link CompletionRequest#topP} but not both.
     */
    Double temperature;

    /**
     * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of
     * the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are
     * considered.
     * We generally recommend using this or {@link CompletionRequest#temperature} but not both.
     */
    @SerializedName("top_p")
    Double topP;

    /**
     * How many completions to generate for each prompt.
     * Because this parameter generates many completions, it can quickly consume your token quota.
     * Use carefully and ensure that you have reasonable settings for {@link CompletionRequest#maxTokens} and {@link CompletionRequest#stop}.
     */
    Integer n;

    /**
     * Whether to stream back partial progress.
     * If set, tokens will be sent as data-only server-sent events as they become available,
     * with the stream terminated by a data: DONE message.
     */
    @Builder.Default
    Boolean stream = false;

    /**
     * Up to 4 sequences where the API will stop generating further tokens.
     * The returned text will not contain the stop sequence.
     */
    List<String> stop;

    /**
     * The maximum number of tokens to generate.
     * Requests can use up to 2048 tokens shared between prompt and completion.
     * (One token is roughly 4 characters for normal English text)
     */
    @SerializedName("max_tokens")
    Integer maxTokens;

    /**
     * Number between 0 and 1 (default 0) that penalizes new tokens based on whether they appear in the text so far.
     * Increases the model's likelihood to talk about new topics.
     */
    @SerializedName("presence_penalty")
    Double presencePenalty;


    /**
     * Number between 0 and 1 (default 0) that penalizes new tokens based on their existing frequency in the text so far.
     * Decreases the model's likelihood to repeat the same line verbatim.
     */
    @SerializedName("frequency_penalty")
    Double frequencyPenalty;


    /**
     * Modify the likelihood of specified tokens appearing in the completion.
     * Maps tokens (specified by their token ID in the GPT tokenizer) to an associated bias value from -100 to 100.
     * https:<a href="//beta.openai.com/docs/api-reference/completions/create#completions/create-logit_bias
     * "></a>
     */
    @SerializedName("logit_bias")
    Map<String, Integer> logitBias;

    /**
     * 是否返回输出令牌的日志概率。如果为true，则返回消息内容中返回的每个输出令牌的日志概率。该选项目前在gpt-4视觉预览模型上不可用。
     */
    Boolean logprobs;

    /**
     * 一个介于0到5之间的整数，指定在每个令牌位置最可能返回的令牌数量，每个令牌都有一个相关的日志概率。
     * 使用此参数时，Logprobs必须设置为true。
     * 简单说，就是每个流返回的字，只返回 最多5个选择
     */
    @SerializedName("top_logprobs")
    Integer topLogprobs;

    /**
     * A unique identifier representing your end-user, which will help OpenAI to monitor and detect abuse.
     */
    String user;

    /**
     * 响应的格式化类类型 （responseFormat 和 responseSchemaClass 二选一）
     */
    @SerializedName("response_format")
    JSONObject responseFormat;

    @SerializedName("stream_options")
    JSONObject streamOptions;

    /**
     * seed 值
     */
    Integer seed;

    @SerializedName("prompt_cache_key")
    String promptCacheKey;

    @SerializedName("prompt_cache_retention")
    String promptCacheRetention;

    @SerializedName("safety_identifier")
    String safetyIdentifier;

    /**
     * 用于  route 类型的 api 框架 LiteLLM 使用
     */
    String secretProperty;

    /**
     * Claude 使用
     */
    @SerializedName("thinking")
    Thinking thinking;

    /**
     * gemini 使用
     */
    @SerializedName("thinkingConfig")
    ThinkingConfig thinkingConfig;

    /**
     * 备用模型列表，当主模型不可用时按顺序尝试 LiteLLM 使用
     * Fallback models to try in order when the primary model is unavailable
     */
    List<String> fallbacks;


    public void addMessage(BaseMessage message) {
        if (CollectionUtils.isEmpty(this.messages)) {
            this.messages = Lists.newArrayList(message);
        } else {
            this.messages.add(message);
        }
    }


    public void addMessageList(List<BaseMessage> messages) {
        if (CollectionUtils.isEmpty(this.messages)) {
            this.messages = Lists.newArrayList();
            this.messages.addAll(messages);
        } else {
            this.messages.addAll(messages);
        }
    }

    public static class ChatRequestBuilder {
        public ChatRequestBuilder addTool(GptTool tool) {
            if (CollectionUtils.isEmpty(this.tools)) {
                this.tools = Lists.newArrayList(tool);
            } else {
                this.tools.add(tool);
            }
            return this;
        }

        public ChatRequestBuilder addMessage(BaseMessage message) {
            if (CollectionUtils.isEmpty(this.messages)) {
                this.messages = Lists.newArrayList(message);
            } else {
                this.messages.add(message);
            }
            return this;
        }


        public ChatRequestBuilder addMessageList(List<BaseMessage> messages) {
            if (CollectionUtils.isEmpty(this.messages)) {
                this.messages = Lists.newArrayList();
                this.messages.addAll(messages);
            } else {
                this.messages.addAll(messages);
            }
            return this;
        }
    }


    /**
     * 声音设置
     */
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @ToString
    public static class AudioSetting {

        /**
         * 声音模型
         */
        String voice;

        /**
         * tts 返回格式
         */
        String format;
    }
}

