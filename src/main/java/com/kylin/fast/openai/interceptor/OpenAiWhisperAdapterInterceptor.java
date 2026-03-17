package com.kylin.fast.openai.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.strategy.KeyStrategyFunction;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class OpenAiWhisperAdapterInterceptor implements Interceptor {

    private final KeyStrategyFunction function;

    public OpenAiWhisperAdapterInterceptor(KeyStrategyFunction function) {
        this.function = function;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        try {
            okhttp3.Response response = chain.proceed(chain.request());
            //适配非json 的转录格式
            if (response.isSuccessful() && chain.request().url().toString().contains("audio/transcriptions")) {
                String body = response.body().string();
                try {
                    JSON temp = JSON.parseObject(body);
                    return response.newBuilder()
                            .body(ResponseBody.create(response.body().contentType(), temp.toJSONString()))
                            .build();
                } catch (Exception e) {
                    //非json 的格式进行兜底
                    return response.newBuilder()
                            .body(ResponseBody.create(response.body().contentType(), new JSONObject()
                                    .fluentPut("text", body)
                                    .toJSONString()))
                            .build();
                }
            }
            function.resultCallBack(response.isSuccessful(), chain.request().header("Authorization"));
            return response;
        } catch (Exception | Error e) {
            function.resultCallBack(false, chain.request().header("Authorization"));
            throw e;
        }
    }
}
