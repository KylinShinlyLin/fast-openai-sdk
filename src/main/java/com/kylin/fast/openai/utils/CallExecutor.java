package com.kylin.fast.openai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylin.fast.openai.exception.OpenAiHttpException;
import com.kylin.fast.openai.result.error.OpenAiError;
import io.reactivex.Single;
import retrofit2.HttpException;

import java.io.IOException;
import java.util.Map;

public class CallExecutor {

    public interface Executor {
        <T> T execute();
    }

    public static <T> T executeWithDot(Map<String, String> labelsParam, Executor apiCall) {
        try {
            return apiCall.execute();
        } catch (Exception e) {
            throw e;
        }
    }

    public static <T> T executeWithDot(Map<String, String> labelsParam, Single<T> apiCall, ObjectMapper mapper) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();
                OpenAiError error = mapper.readValue(errorBody, OpenAiError.class);
                throw new OpenAiHttpException(error, e, e.code());
            } catch (IOException ex) {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        }
    }
}
