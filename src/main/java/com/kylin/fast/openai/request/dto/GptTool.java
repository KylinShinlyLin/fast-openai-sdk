package com.kylin.fast.openai.request.dto;

import lombok.*;

/**
 * Created by ZengShiLin on 2023/6/14 10:19 AM
 *
 * @author ZengShiLin
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class GptTool {

    String id;

    GptFunction function;
    /**
     * TODO openai 后面可能会有更多的工具
     * 普通函数触发就使用 function 填充
     * The type of the tool. Currently, only function is supported.
     */
    String type;


    public static GptTool of(GptFunction function) {
        return GptTool.builder()
                .function(function)
                .type("function")
                .build();
    }
}
