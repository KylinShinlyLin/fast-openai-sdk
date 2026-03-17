package com.kylin.fast.jina.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface JinaApi {

    @GET
    Call<ResponseBody> fetch(@Url String url);
}
