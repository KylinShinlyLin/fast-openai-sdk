package com.kylin.fast.openai.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 描述：金额消耗列表
 */
@Data
@ToString
public class DailyCost {
    /**
     * 时间戳
     */
    @JsonProperty("timestamp")
    private long timestamp;
    /**
     * 模型消耗金额详情
     */
    @JsonProperty("line_items")
    private List<LineItem> lineItems;

}
