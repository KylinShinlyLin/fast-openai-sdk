package com.kylin.fast.openai.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
public class OpenAiErrorInfoInterceptor implements Interceptor {

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        okhttp3.Response response = chain.proceed(chain.request());
        if (response.code() != 200) {
            int code = response.code();
            ResponseBody body = response.body();
            if (body != null) {
                byte[] content = body.bytes();
                MediaType mediaType = body.contentType();
                String message = new String(content, mediaType != null ? Objects.requireNonNull(mediaType.charset(StandardCharsets.UTF_8)) : StandardCharsets.UTF_8);

                String url = chain.request().url().toString();
                log.warn("openai request failed code:{} error message:{} url:{}", code, message, url);
                System.err.printf("openai request failed code:%s error message:%s url:%s%n", code, message, url);

                return response.newBuilder()
                        .body(ResponseBody.create(mediaType, content))
                        .build();
            }
        }
        return response;
    }
}
