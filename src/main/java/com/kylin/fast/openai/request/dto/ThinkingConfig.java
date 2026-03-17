package com.kylin.fast.openai.request.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThinkingConfig {

    /**
     * think tokens 上限
     */
    Boolean includeThoughts;

    Integer thinkingBudget;
    /**
     * think 类型
     * enabled / disabled
     */
    String thinkingLevel;
}
