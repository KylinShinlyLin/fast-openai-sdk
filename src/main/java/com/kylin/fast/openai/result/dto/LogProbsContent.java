package com.kylin.fast.openai.result.dto;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Created by ZengShiLin on 2024/1/12 09:52
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class LogProbsContent {

    String token;

    Double logprob;

    Byte[] bytes;

    @SerializedName("top_logprobs")
    List<Toplogprobs> topLogprobs;
}
