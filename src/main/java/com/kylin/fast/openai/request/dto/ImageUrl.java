package com.kylin.fast.openai.request.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageUrl {

    String url;

    /**
     * low or high
     */
    String detail;
}
