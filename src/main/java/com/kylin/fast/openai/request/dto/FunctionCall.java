package com.kylin.fast.openai.request.dto;

import lombok.*;

/**
 * Created by ZengShilin on 2023/6/14 11:19 AM
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class FunctionCall {

    /**
     * 插件名字
     */
    String name;

    /**
     * 通过NLP 提取出来的参数
     * Openai 返回的格式有点问题，是字符串json，导致无法直接使用类承接
     * {\n  \"location\": \"Boston, MA\"\n}
     * 本质上入参是 json
     */
    String arguments;
}
