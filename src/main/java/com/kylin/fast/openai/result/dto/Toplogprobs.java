package com.kylin.fast.openai.result.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Created by ZengShiLin on 2024/1/12 09:53
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Toplogprobs {

    String token;

    Double logprob;

    Byte[] bytes;
}
