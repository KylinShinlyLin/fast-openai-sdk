package com.kylin.fast.openai.result.dto;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * The OpenAI resources used by a request
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Usage {
    /**
     * The number of prompt tokens used.
     */
    @SerializedName("prompt_tokens")
    long promptTokens;

    /**
     * The number of completion tokens used.
     */
    @SerializedName("completion_tokens")
    long completionTokens;

    /**
     * The number of total tokens used
     */
    @SerializedName("total_tokens")
    long totalTokens;

    PromptTokensDetails prompt_tokens_details;


    CompletionTokensDetails completion_tokens_details;

    @Data
    @Builder
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PromptTokensDetails {
        @SerializedName("cached_tokens")
        long cachedTokens;

        @SerializedName("audio_tokens")
        long audioTokens;
    }


    @Data
    @Builder
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CompletionTokensDetails {
        @SerializedName("reasoning_tokens")
        long reasoningTokens;
        @SerializedName("audio_tokens")
        long audioTokens;
        @SerializedName("accepted_prediction_tokens")
        long acceptedPredictionTokens;
        @SerializedName("rejected_prediction_tokens")
        long rejectedPredictionTokens;
    }
}
