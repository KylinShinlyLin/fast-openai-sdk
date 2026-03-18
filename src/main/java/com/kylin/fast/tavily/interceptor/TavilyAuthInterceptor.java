package com.kylin.fast.tavily.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class TavilyAuthInterceptor implements Interceptor {

    private final String apiKey;

    public TavilyAuthInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey);

        return chain.proceed(builder.build());
    }
}
