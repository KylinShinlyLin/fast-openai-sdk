package com.kylin.fast.jina.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class JinaAuthInterceptor implements Interceptor {

    private final String apiKey;

    public JinaAuthInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header("Authorization", "Bearer " + apiKey)
                //开启浏览器代理
                .header("X-User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("X-Engine", "cf-browser-rendering")
                //查看嵌入页面
                .header("X-With-Iframe", "true")
                .header("X-With-Shadow-Dom", "true");

        return chain.proceed(builder.build());
    }
}
