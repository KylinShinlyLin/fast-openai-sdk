package com.kylin.fast.openai.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylin.fast.openai.exception.OpenAiHttpException;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.result.error.OpenAiError;
import com.kylin.fast.openai.utils.JsonUtil;
import io.reactivex.FlowableEmitter;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.CollectionUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

/**
 * Callback to parse Server Sent Events (SSE) from raw InputStream and
 * emit the events with io.reactivex.FlowableEmitter to allow streaming of
 * SSE.
 */
public class ResponseBodyCallback implements Callback<ResponseBody> {
    private static final ObjectMapper mapper = OpenAiService.defaultObjectMapper();

    private FlowableEmitter<SSE> emitter;
    private boolean emitDone;

    public ResponseBodyCallback(FlowableEmitter<SSE> emitter, boolean emitDone) {
        this.emitter = emitter;
        this.emitDone = emitDone;
    }

    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        BufferedReader reader = null;
        try {
            if (!response.isSuccessful()) {
                HttpException e = new HttpException(response);
                ResponseBody errorBody = response.errorBody();

                if (errorBody == null) {
                    throw e;
                } else {
                    OpenAiError error = mapper.readValue(
                            errorBody.string(),
                            OpenAiError.class
                    );
                    throw new OpenAiHttpException(error, e, e.code());
                }
            }

            InputStream in = response.body().byteStream();
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            SSE sse;

            while ((line = reader.readLine()) != null) {
                List<String> jsons = JsonUtil.getJsonFromString(line);
                if (!CollectionUtils.isEmpty(jsons)) {
                    for (String str : jsons) {
                        if (Objects.isNull(str)) {
                            continue;
                        }
                        sse = new SSE(str);
                        emitter.onNext(sse);
                    }
                }
                if (line.contains("data: [DONE]")) {
                    emitter.onComplete();
                }

//                if (line.startsWith("data:")) {
//                    String data = line.substring(5).trim();
//                    sse = new SSE(data);
//                } else if ("".equals(line) && sse != null) {
//                    if (sse.isDone()) {
//                        if (emitDone) {
//                            emitter.onNext(sse);
//                        }
//                        break;
//                    }
//
//                    emitter.onNext(sse);
//                    sse = null;
//                } else {
//                    throw new SSEFormatException("Invalid sse format! " + line);
//                }
            }

        } catch (Throwable t) {
            onFailure(call, t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
        emitter.onError(t);
    }
}