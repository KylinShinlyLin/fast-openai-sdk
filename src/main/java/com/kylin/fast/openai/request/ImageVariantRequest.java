package com.kylin.fast.openai.request;

import lombok.*;

/**
 * @see <a href="https://platform.openai.com/docs/api-reference/images/create-edit">...</a>
 * Created by ZengShilin on 2023/3/2 5:27 PM
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ImageVariantRequest {


//    String prompt;

    String model;

    /**
     * The number of images to generate. Must be between 1 and 10.
     */
    Integer n;

    /**
     * The size of the generated images. Must be one of 256x256, 512x512, or 1024x1024.
     */
    String size;

    /**
     * The format in which the generated images are returned. Must be one of url or b64_json.
     */
    String responseFormat;

    /**
     * A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse. Learn more.
     */
    String user;

}
