package com.kylin.fast.openai.api;


import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.request.*;
import com.kylin.fast.openai.result.*;
import io.reactivex.Single;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.time.LocalDate;
import java.util.Map;


public interface OpenAiApi {

    @POST("/v1/moderations")
    Single<JSONObject> moderations(@Body ModerationsRequest request);

    @POST("/v1/completions")
    Single<CompletionResult> createCompletion(@Body CompletionRequest request);

    @POST("/v1/chat/completions")
    Single<ChatResult> createChat(@Body ChatRequest request);

    @POST("/v1/chat/completions")
    Single<ChatResult> createChat(@Body ChatRequest request, @HeaderMap Map<String, String> headers);

    @Streaming
    @POST("/v1/chat/completions")
    Call<ResponseBody> createChatStream(@Body ChatRequest request);

    @Streaming
    @POST("/v1/chat/completions")
    Call<ResponseBody> createChatStream(@Body ChatRequest request, @HeaderMap Map<String, String> headers);

    @POST("/v1/chat/completions")
    Single<ChatResult> createChat(@Body ChatRequest request, @HeaderMap Map<String, String> headers, @QueryMap Map<String, String> queryParams);

    @Streaming
    @POST("/v1/chat/completions")
    Call<ResponseBody> createChatStream(@Body ChatRequest request, @HeaderMap Map<String, String> headers, @QueryMap Map<String, String> queryParams);


    @POST("/v1/images/generations")
    Single<ImageResult> createImage(@Body ImageRequest request);

    @POST("/v1/images/edits")
    Single<ImageResult> imageEdits(@Body RequestBody requestBody);

    @Multipart
    @POST("v1/images/edits")
    Single<ImageResult> editImages(@Part() MultipartBody.Part image,
                                   @Part() MultipartBody.Part mask,
                                   @PartMap() Map<String, RequestBody> requestBodyMap
    );

    @POST("/v1/images/variations")
    Single<ImageResult> imageVariant(@Body RequestBody requestBody);


    @POST("/v1/audio/transcriptions")
    Single<WhisperResult> audioText(@Body RequestBody requestBody);


    @POST("/v1/audio/speech")
    @Headers({"Content-Type: application/json"})
    Single<ResponseBody> textToSpeech(@Body SpeechRequest request);

    @POST("/v1/embeddings")
    Single<EmbeddingResult> createEmbeddings(@Body EmbeddingRequest request);


    @GET("v1/dashboard/billing/usage")
    Single<BillingResult> billingDaily(@Query("start_date") LocalDate starDate, @Query("end_date") LocalDate endDate);



}
