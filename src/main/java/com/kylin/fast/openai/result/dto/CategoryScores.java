package com.kylin.fast.openai.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Created by ZengShilin on 2023/3/10 10:49 AM
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class CategoryScores {


    double hate;

    @JsonProperty(value = "hate/threatening")
    double hateThreatening;

    @JsonProperty(value = "self-harm")
    double selfHarm;

    double sexual;

    @JsonProperty(value = "sexual/minors")
    double sexualMinors;

    double violence;

    @JsonProperty(value = "violence/graphic")
    double violenceGraphic;

}
