package com.kylin.fast.openai.result;

import com.kylin.fast.openai.result.dto.CompletionChoice;
import com.kylin.fast.openai.result.dto.Usage;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * An object containing a response from the completion api
 * <p>
 * https://beta.openai.com/docs/api-reference/completions/create
 */
@Data
@ToString
public class CompletionResult {
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
    List<CompletionChoice> choices;

    /**
     * The API usage for this request
     */
    Usage usage;
}
