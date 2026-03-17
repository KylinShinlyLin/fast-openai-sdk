package com.kylin.fast.openai.result.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * Represents an embedding returned by the embedding api
 * <p>
 * https://beta.openai.com/docs/api-reference/classifications/create
 */
@Data
@ToString
public class Embedding {


    /**
     * The embedding vector
     */
    List<Float> embedding;
}
