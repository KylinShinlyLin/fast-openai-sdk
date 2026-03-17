package com.kylin.fast.openai.result.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * 描述：金额消耗列表
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class LineItem {
    /**
     * 模型名称
     */
    private String name;
    /**
     * 消耗金额 美分
     */
    private BigDecimal cost;
}
