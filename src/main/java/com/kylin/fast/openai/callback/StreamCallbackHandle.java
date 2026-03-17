package com.kylin.fast.openai.callback;

import com.alibaba.fastjson.JSON;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.result.ChatStreamResult;
import com.kylin.fast.openai.result.dto.ChatCompletionStreamChoice;
import com.kylin.fast.openai.result.dto.ChatMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * openai 流式适配 封装目标
 * 1、封装好 openai 各种场景下的返回值
 * 2、支持usage 的返回
 * 3、支持流式函数调用
 * 4、支持流式 jmx 监控（首字耗时，整体耗时）
 */
@Slf4j
public class StreamCallbackHandle implements Callback<ResponseBody> {

    private final CountDownLatch countDownLatch;
    private final long startTime;
    private final String instanceName;
    private final ChatRequest request;
    private OpenAiService.GptExecutor executor;

    private final AtomicBoolean receivedFirstContent;

    private final StringBuilder responseStream;

    public StreamCallbackHandle(CountDownLatch countDownLatch,
                                ChatRequest request,
                                OpenAiService.GptExecutor executor,
                                String instanceName) {
        this.countDownLatch = countDownLatch;
        this.startTime = System.currentTimeMillis();
        this.request = request;
        this.executor = executor;
        this.instanceName = instanceName;
        this.receivedFirstContent = new AtomicBoolean(false);
        this.responseStream = new StringBuilder();
    }


    @SneakyThrows
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        String line;
        try (InputStream in = response.body().byteStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            while ((line = reader.readLine()) != null && countDownLatch.getCount() > 0) {
                if (StringUtils.isBlank(line) || !line.startsWith("data:")) {
                    continue;
                }
                //提取 openai data 中的 json 数据
                String json = truncateHeadString(line, 6);
//                System.err.println(json);
                ChatStreamResult result = JSON.parseObject(json, ChatStreamResult.class);
                if (CollectionUtils.isNotEmpty(result.getChoices()) &&
                        result.getChoices().stream()
                                .map(ChatCompletionStreamChoice::getFinish_reason)
                                .filter(Objects::nonNull)
                                .anyMatch(s -> s.equals("stop"))) {
                    //过滤掉无效内容 这时候是触发函数
                    continue;
                }
//                //累计回复，进行统计
//                String resultStr = result.getChoices()
//                        .stream()
//                        .filter(Objects::nonNull)
//                        .map(ChatCompletionStreamChoice::getDelta)
//                        .map(ChatMessage::getContent)
//                        .collect(Collectors.joining());

//                if (!receivedFirstContent.get() && StringUtils.isNotBlank(resultStr)) {
//                    //第一个字打点
//                    long costTime = System.currentTimeMillis() - startTime;
//                    Map<String, String> labelsParam = new HashMap<>();
//                    labelsParam.put("isStream", String.valueOf(request.getStream()));
//                    labelsParam.put("model", request.getModel());
//                    labelsParam.put("api", "chatStream");
//                    labelsParam.put("type", "firstReceived");
//                    labelsParam.put("instanceName", instanceName);
//                    receivedFirstContent.getAndSet(true);
//                }

                if (CollectionUtils.isEmpty(result.getChoices()) && Objects.isNull(result.getUsage())) {
                    //choices 一般不可能为空
                    log.warn("openai stream chuck empty line:{}", line);
                } else if (Objects.nonNull(result.getUsage())) {
                    //流式当有 usage 的时候就可以判定为结束
                    executor.handle(result, true);
                    countDownLatch.countDown();
                    long costTime = System.currentTimeMillis() - startTime;
                    Map<String, String> labelsParam = new HashMap<>();
                    labelsParam.put("isStream", String.valueOf(request.getStream()));
                    labelsParam.put("model", request.getModel());
                    labelsParam.put("api", "chatStream");
                    labelsParam.put("instanceName", instanceName);
                    return;
                } else {
                    //非空和函数调用会直接返回
//                    if (!resultStr.isEmpty() || Optional.of(result)
//                            .map(ChatStreamResult::getChoices)
//                            .map(e -> e.get(0))
//                            .map(ChatCompletionStreamChoice::getDelta)
//                            .filter(e -> !CollectionUtils.isEmpty(e.getToolCalls()))
//                            .isPresent()) {
//                        executor.handle(result, false);
//                    }
                    executor.handle(result, false);
                }
            }
        } catch (Exception e) {
            log.error("exception:", e);
            long costTime = System.currentTimeMillis() - startTime;
            Map<String, String> labelsParam = new HashMap<>();
            labelsParam.put("isStream", String.valueOf(request.getStream()));
            labelsParam.put("model", request.getModel());
            labelsParam.put("api", "chatStream");
            labelsParam.put("instanceName", instanceName);
            countDownLatch.countDown();
            throw e;
        } finally {
            //结束 sse
            call.cancel();
        }
    }


    @SneakyThrows
    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
        // 流式异常抛出，暂时不处理
        long costTime = System.currentTimeMillis() - startTime;
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("isStream", String.valueOf(request.getStream()));
        labelsParam.put("model", request.getModel());
        labelsParam.put("api", "chatStream");
        labelsParam.put("instanceName", instanceName);
        countDownLatch.countDown();
        throw t;
    }


    public static String truncateHeadString(String origin, int count) {
        if (origin == null || origin.length() < count) {
            return null;
        }
        char[] arr = origin.toCharArray();
        char[] ret = new char[arr.length - count];
        System.arraycopy(arr, count, ret, 0, ret.length);
        return String.copyValueOf(ret);
    }
}
