package com.kylin.fast.openai.result;


import com.kylin.fast.openai.result.dto.Embedding;
import com.kylin.fast.openai.result.dto.Usage;
import lombok.Data;

import java.util.List;

/**
 * An object containing a response from the answer api
 * <p>
 * https://beta.openai.com/docs/api-reference/embeddings/create
 */
@Data
public class EmbeddingResult {

    /**
     * The GPT-3 model used for generating embeddings
     */
    String model;

    /**
     * The type of object returned, should be "list"
     */
    String object;

    /**
     * A list of the calculated embeddings
     */
    List<Embedding> data;

    /**
     * The API usage for this request
     */
    Usage usage;
}
