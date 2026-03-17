package com.kylin.fast.openai.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Created by ZengShilin on 2023/3/10 10:46 AM
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Categories {

    boolean hate;

    @JsonProperty(value = "hate/threatening")
    boolean hateThreatening;

    @JsonProperty(value = "self-harm")
    boolean selfHarm;

    boolean sexual;

    @JsonProperty(value = "sexual/minors")
    boolean sexualMinors;

    boolean violence;

    @JsonProperty(value = "violence/graphic")
    boolean violenceGraphic;
}
