package com.kylin.fast.openai.interceptor;

import com.kylin.fast.openai.strategy.KeyStrategyFunction;
import lombok.Getter;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OkHttp Interceptor that adds an authorization token header
 */
public class OpenAiAuthInterceptor implements Interceptor {

    private final KeyStrategyFunction keyStrategyFunction;


    @Getter
    private final ConcurrentHashMap<String, AtomicLong> keyUseCount = new ConcurrentHashMap<>();

    public OpenAiAuthInterceptor(KeyStrategyFunction function) {
        this.keyStrategyFunction = function;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String key = keyStrategyFunction.routing();
        Request request = chain.request()
                .newBuilder()
                .header("Authorization", "Bearer " + key)
                .build();
        String keySuffix = key.substring(key.length() - 4);
        if (!keyUseCount.containsKey(keySuffix)) {
            keyUseCount.putIfAbsent(keySuffix, new AtomicLong());
        }
        keyUseCount.computeIfPresent(keySuffix, (s, atomicLong) -> {
            atomicLong.incrementAndGet();
            return atomicLong;
        });
        return chain.proceed(request);
    }
}
