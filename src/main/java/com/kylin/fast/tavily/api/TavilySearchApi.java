package com.kylin.fast.tavily.api;

import com.kylin.fast.tavily.request.TavilySearchRequest;
import com.kylin.fast.tavily.response.TavilySearchResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TavilySearchApi {

    @POST("search")
    Call<TavilySearchResponse> search(@Body TavilySearchRequest request);
}
