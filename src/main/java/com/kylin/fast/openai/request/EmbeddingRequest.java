package com.kylin.fast.openai.request;

import lombok.*;

import java.util.List;

/**
 * Creates an embedding vector representing the input text.
 * <p>
 * https://beta.openai.com/docs/api-reference/embeddings/create
 * @author zengming
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class EmbeddingRequest {

    /**
     * The name of the model to use.
     * Required if using the new v1/embeddings endpoint.
     */
    String model;

    /**
     * Input text to get embeddings for, encoded as a string or array of tokens.
     * To get embeddings for multiple inputs in a single request, pass an array of strings or array of token arrays.
     * Each input must not exceed 2048 tokens in length.
     * <p>
     * Unless your are embedding code, we suggest replacing newlines (\n) in your input with a single space,
     * as we have observed inferior results when newlines are present.
     */

    List<String> input;

    /**
     * A unique identifier representing your end-user, which will help OpenAI to monitor and detect abuse.
     */
    String user;

    /**
     * The number of dimensions the resulting output embeddings should have. Only supported in text-embedding-3 and later models.
     */
    Integer dimensions;

    /**
     * The format to return the embeddings in. Can be either
     * [float] or [base64].
     */
    String encoding_format;
}
