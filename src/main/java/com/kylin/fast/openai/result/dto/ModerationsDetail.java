package com.kylin.fast.openai.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Created by ZengShilin on 2023/3/10 10:45 AM
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class ModerationsDetail {

    Categories categories;

    @JsonProperty(value = "category_scores")
    CategoryScores categoryScores;

    boolean flagged;
}
