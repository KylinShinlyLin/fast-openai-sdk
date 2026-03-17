package com.kylin.fast.openai.request.dto;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImgMessageContent {

    /**
     * 内容类型 text/image_url
     */
    String type;

    /**
     * 与文本聊天的内容
     */
    String text;

    /**
     * 图片地址
     */
    @SerializedName("image_url")
    ImageUrl image_url;
}
