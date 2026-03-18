package com.kylin.fast.tavily.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.tavily.interceptor.TavilyAuthInterceptor;
import com.kylin.fast.tavily.request.TavilySearchRequest;
import com.kylin.fast.tavily.response.TavilySearchResponse;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.net.Proxy;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TavilySearchService {

    private final String baseUrl;
    private final TavilySearchApi api;

    public TavilySearchService(OpenAiConfig config) {
        this(config.getTavilyApiKey(), config.getTavilyBaseUrl(), config.getTimeout(), config.getProxy());
    }

    public TavilySearchService(String apiKey) {
        this(apiKey, "https://api.tavily.com/", Duration.ofSeconds(30), null);
    }

    public TavilySearchService(String apiKey, Duration timeout) {
        this(apiKey, "https://api.tavily.com/", timeout, null);
    }

    public TavilySearchService(String apiKey, String baseUrl, Duration timeout, Proxy proxy) {
        this.baseUrl = baseUrl;
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new TavilyAuthInterceptor(apiKey))
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.MINUTES))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);

        if (proxy != null) {
            clientBuilder.proxy(proxy);
        }

        OkHttpClient client = clientBuilder.build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Optional.ofNullable(baseUrl).orElse("https://api.tavily.com/"))
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build();

        this.api = retrofit.create(TavilySearchApi.class);
    }

    /**
     * Search Tavily using a query string.
     *
     * @param query The search query string
     * @return Search results
     * @throws IOException If the request fails
     */
    public TavilySearchResponse search(String query) throws IOException {
        TavilySearchRequest request = TavilySearchRequest.builder()
                .query(query)
                .searchDepth("advanced")
                .build();
        return search(request);
    }

    /**
     * Search Tavily using an explicit request object.
     *
     * @param request The complete search request configuration
     * @return Search results
     * @throws IOException If the request fails
     */
    public TavilySearchResponse search(TavilySearchRequest request) throws IOException {
        Call<TavilySearchResponse> call = api.search(request);
        retrofit2.Response<TavilySearchResponse> response = call.execute();

        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else {
            String errorMsg = "Tavily Search API request failed: " + response.code();
            if (response.errorBody() != null) {
                errorMsg += " - " + response.errorBody().string();
            }
            throw new IOException(errorMsg);
        }
    }
}
