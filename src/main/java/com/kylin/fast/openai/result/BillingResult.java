package com.kylin.fast.openai.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylin.fast.openai.result.dto.DailyCost;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * 描述：金额消耗信息
 *
 */
@Data
@ToString
public class BillingResult {

    @JsonProperty("object")
    private String object;
    /**
     * 账号金额消耗明细
     */
    @JsonProperty("daily_costs")
    private List<DailyCost> dailyCosts;
    /**
     * 总使用金额：美分
     */
    @JsonProperty("total_usage")
    private BigDecimal totalUsage;

}
