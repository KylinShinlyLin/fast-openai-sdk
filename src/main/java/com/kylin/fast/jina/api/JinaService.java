package com.kylin.fast.jina.api;

import com.kylin.fast.jina.interceptor.JinaAuthInterceptor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.io.IOException;
import java.net.Proxy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class JinaService {

    private static final String BASE_URL = "https://r.jina.ai/";
    private final JinaApi api;

    public JinaService(String apiKey) {
        this(apiKey, Duration.ofSeconds(30), null);
    }

    public JinaService(String apiKey, Duration timeout) {
        this(apiKey, timeout, null);
    }

    public JinaService(String apiKey, Duration timeout, Proxy proxy) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new JinaAuthInterceptor(apiKey))
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.MINUTES))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);

        if (proxy != null) {
            clientBuilder.proxy(proxy);
        }

        OkHttpClient client = clientBuilder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .build();

        this.api = retrofit.create(JinaApi.class);
    }

    /**
     * 将指定 URL 的网页内容转换为 Markdown
     *
     * @param url 目标网页 URL
     * @return Markdown 内容
     * @throws IOException 请求失败时抛出
     */
    public String fetchMarkdown(String url) throws IOException {
        // Jina Reader API 格式: https://r.jina.ai/{url}
        // Retrofit @Url 会直接使用传入的完整 URL
        String fullUrl = BASE_URL + url;

        Call<ResponseBody> call = api.fetch(fullUrl);
        retrofit2.Response<ResponseBody> response = call.execute();

        if (response.isSuccessful() && response.body() != null) {
            return response.body().string();
        } else {
            String errorMsg = "Jina Reader API request failed: " + response.code();
            if (response.errorBody() != null) {
                errorMsg += " - " + response.errorBody().string();
            }
            throw new IOException(errorMsg);
        }
    }
}
