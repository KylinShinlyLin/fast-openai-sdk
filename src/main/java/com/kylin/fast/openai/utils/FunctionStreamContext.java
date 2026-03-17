package com.kylin.fast.openai.utils;

import com.google.common.collect.Lists;
import com.kylin.fast.openai.request.dto.GptTool;
import com.kylin.fast.openai.result.ChatStreamResult;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 由于现在流式触发函数，也是通过流式返回参数
 * 设计这个工具，用于接收触发的函数调用，包括并行函数调用
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FunctionStreamContext {

    @Getter
    List<FunctionTrigger> functionArguments = Lists.newArrayList();

    private String currentFunctionName;

    private StringBuilder arguments = new StringBuilder();

    public void streamAppend(ChatStreamResult result) {
        if (StringUtils.isNotBlank(result.getFunctionName())) {
            GptTool gptTool = result.getChoices().get(0).getDelta().getToolCalls().get(0);
            arguments = new StringBuilder();
            functionArguments.add(FunctionTrigger.builder()
                    .id(gptTool.getId())
                    .name(gptTool.getFunction().getName())
                    .arguments(arguments)
                    .build());
        }

        if (result.triggerFunction()) {
            arguments.append(result.functionArguments());
        }

    }


}
