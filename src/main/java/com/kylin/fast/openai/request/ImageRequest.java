package com.kylin.fast.openai.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class ImageRequest {

    /**
     * A text description of the desired image(s). The maximum length is 1000 characters.
     */
    String prompt;

    /**
     * The number of images to generate. Must be between 1 and 10.
     */
    Integer n;

    String model;

    /**
     * 只有 DALL-E 3 模型支持
     * hd / standard
     */
    String quality;

    /**
     * The size of the generated images. Must be one of 256x256, 512x512, or 1024x1024.
     */
    String size;

    /**
     * The style of the generated images. Must be one of `vivid` or `natural`
     */
    String style;

    /**
     * The format in which the generated images are returned. Must be one of url or b64_json.
     */
    String responseFormat;

    /**
     * A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse. Learn more.
     */
    String user;

//    Long seed;
}
