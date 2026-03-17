package com.kylin.fast.openai.request.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.annotations.SerializedName;
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
public class GptFunction {

    /**
     * 插件名称，唯一且确定
     */
    String name;

    /**
     * 你的插件描述。相当于prompt
     * 决定这个插件何时被调用
     */
    String description;

    /**
     * 你的参数定义，需要NLP 解析的参数
     * 这部分配置相当于是 prompt
     */
    @SerializedName("parameters")
    JSONObject parameters;

    /**
     * tools call 和 function call 参数不一致
     * tool call 参数
     */
    String arguments;

    /**
     * 是否在生成函数调用时启用严格的模式遵循。如果设置为真，模型将按照参数字段中定义的确切模式进行操作。只有当严格模式为真时，才支持JSON模式的一部分。
     * 在函数调用指南中了解更多关于结构化输出的信息。
     */
    Boolean strict;


}
