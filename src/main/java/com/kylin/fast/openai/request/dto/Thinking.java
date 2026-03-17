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
public class Thinking {

    /**
     * think tokens 上限
     */
    Integer budget_tokens;

    /**
     * think 类型
     * enabled / disabled
     */
    String type;
}
