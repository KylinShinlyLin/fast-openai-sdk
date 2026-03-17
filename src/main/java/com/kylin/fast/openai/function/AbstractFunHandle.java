package com.kylin.fast.openai.function;

import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.request.ChatRequest;

/**
 * 函数调用抽象类
 */
public abstract class AbstractFunHandle {

    /**
     * 函数名字
     *
     * @return String
     */
    public abstract String functionName();

    /**
     * 针对当时函数调用的参数
     *
     * @return description
     */
    public abstract String description();

    /**
     * 定义的函数调用 参数类型
     *
     * @return Class<?>
     */
    public abstract JSONObject parametersType();

    /**
     * 触发函数调用
     *
     * @param param   T 类型的入参
     * @param request 请求 GPT 时候带的 request
     */
    public abstract void handle(JSONObject param, ChatRequest request, String id);

}
