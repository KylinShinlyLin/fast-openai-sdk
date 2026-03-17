
package com.kylin.fast.openai.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kylin.fast.openai.exception.OpenAiHttpException;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.result.error.OpenAiError;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 */
public abstract class AbstractStreamBodyCallback implements Callback<ResponseBody> {
    private static final ObjectMapper mapper = OpenAiService.defaultObjectMapper();


    private static final String DONE_DATA = "[DONE]";

    public AbstractStreamBodyCallback() {

    }


    public abstract void streamText(String text, boolean isDone);


    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        BufferedReader reader = null;

        try {
            //如果失败，尝试获取异常信息
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

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                }

                streamText(line, false);

//                else if ("".equals(line) && sse != null) {
//                    if (isDone()) {
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

//            emitter.onComplete();

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

//    public boolean isDone() {
//        return DONE_DATA.equalsIgnoreCase(this.data);
//    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
//        emitter.onError(t);
    }
}