package com.kylin.fast.openai.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Lists;
import com.kylin.fast.openai.callback.StreamCallbackFunctionHandle;
import com.kylin.fast.openai.callback.StreamCallbackHandle;
import com.kylin.fast.openai.function.AbstractFunHandle;
import com.kylin.fast.openai.function.AnnotationFunHandle;
import com.kylin.fast.openai.function.FunctionContextHandler;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.handler.BaseFunctionHandler;
import com.kylin.fast.openai.interceptor.OpenAiAuthInterceptor;
import com.kylin.fast.openai.interceptor.OpenAiErrorInfoInterceptor;
import com.kylin.fast.openai.interceptor.OpenAiWhisperAdapterInterceptor;
import com.kylin.fast.openai.request.*;
import com.kylin.fast.openai.request.dto.BaseMessage;
import com.kylin.fast.openai.request.dto.GptFunction;
import com.kylin.fast.openai.request.dto.GptTool;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.result.*;
import com.kylin.fast.openai.result.dto.ChatChoice;
import com.kylin.fast.openai.strategy.KeyRandomStrategy;
import com.kylin.fast.openai.strategy.KeyStrategyFunction;
import com.kylin.fast.openai.utils.CallExecutor;
import com.kylin.fast.openai.utils.FunctionTrigger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;

/**
 * Created by ZengShilin on 2023/3/2 10:31 AM
 */
@Slf4j
public class OpenAiService {

    private static final String BASE_URL = "https://api.openai.com";

    private static final String DALL_E_3 = "dall-e-3";

    private final OpenAiApi api;

    private static final ObjectMapper mapper = defaultObjectMapper();

    private final KeyStrategyFunction function;

    private final Duration timeout;

    private final String instanceName;

    private final Retryer<Object> retryer;

    public OpenAiService(com.kylin.fast.openai.config.OpenAiConfig config) {
        this(new com.kylin.fast.openai.strategy.KeyRandomStrategy(config.getApiKeys()),
             config.getTimeout(), 
             config.getBaseUrl(), 
             "Default", 
             config.getProxy(), 
             config.getMaxRetries());
    }

    public OpenAiService(final List<String> tokens) {
        this(new KeyRandomStrategy(tokens), ofSeconds(20), BASE_URL, "Default", null, 3);
    }

    public OpenAiService(final List<String> tokens, final String host) {
        this(new KeyRandomStrategy(tokens), ofSeconds(20), host, "Default", null, 3);
    }

    public OpenAiService(final List<String> tokens, final Duration timeout) {
        this(new KeyRandomStrategy(tokens), timeout, BASE_URL, "Default", null, 3);
    }

    public OpenAiService(String host, final List<String> tokens, final Duration timeout) {
        this(new KeyRandomStrategy(tokens), timeout, host, "Default", null, 3);
    }

    public OpenAiService(final List<String> tokens, final String host, final Duration timeout) {
        this(new KeyRandomStrategy(tokens), timeout, host, "Default", null, 3);
    }

    public OpenAiService(final List<String> tokens, final Duration timeout, int retry) {
        this(new KeyRandomStrategy(tokens), timeout, BASE_URL, "Default", null, retry);
    }

    public OpenAiService(final List<String> tokens, final Duration timeout, Proxy proxy) {
        this(new KeyRandomStrategy(tokens), timeout, BASE_URL, "Default", proxy, 3);
    }


    public OpenAiService(final List<String> tokens, final String host, final Duration timeout, Proxy proxy) {
        this(new KeyRandomStrategy(tokens), timeout, host, "Default", proxy, 3);
    }

    public OpenAiService(final List<String> tokens, final Duration timeout, Proxy proxy, int retry) {
        this(new KeyRandomStrategy(tokens), timeout, BASE_URL, "Default", proxy, retry);
    }

    public OpenAiService(final List<String> tokens, final Duration timeout, String instanceName) {
        this(new KeyRandomStrategy(tokens), timeout, BASE_URL, instanceName, null, 3);
    }

    public OpenAiService(final List<String> tokens, final Duration timeout, String instanceName, Proxy proxy) {
        this(new KeyRandomStrategy(tokens), timeout, BASE_URL, instanceName, proxy, 3);
    }

    public OpenAiService(final List<String> tokens, final int timeout, String url) {
        this(new KeyRandomStrategy(tokens), ofSeconds(timeout), url, "Default", null, 3);
    }

    public OpenAiService(KeyStrategyFunction function, final Duration timeout,
                         String url, String instanceName, int retryMaxTime, OkHttpClient okHttpClient) {
        this.function = function;
        this.timeout = timeout;
        this.instanceName = instanceName;

        //重试组件
        retryer = RetryerBuilder.newBuilder()
                .retryIfResult(Objects::isNull)
                .retryIfExceptionOfType(Exception.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.fibonacciWait(3000, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(retryMaxTime))
                .build();

        // api 映射
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Optional.ofNullable(url).orElse(BASE_URL))
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        this.api = retrofit.create(OpenAiApi.class);
    }

    public OpenAiService(KeyStrategyFunction function, final Duration timeout,
                         String url, String instanceName, Proxy proxy, int retryMaxTime) {
        this.function = function;
        this.timeout = timeout;
        this.instanceName = instanceName;
        ConnectionPool pool = new ConnectionPool(1024, 10, TimeUnit.SECONDS);
        //重试组件
        retryer = RetryerBuilder.newBuilder()
                .retryIfResult(Objects::isNull)
                .retryIfExceptionOfType(Exception.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.fibonacciWait(3000, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(retryMaxTime))
                .build();

        OpenAiAuthInterceptor interceptor = new OpenAiAuthInterceptor(this.function);
        //OkHttp 初始化
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .proxy(proxy)
                //请求失败的日志
                .addInterceptor(new OpenAiErrorInfoInterceptor())
                .addInterceptor(interceptor)
                //whisper 需要对 srt 和 vrt 格式进行适配
                .addInterceptor(new OpenAiWhisperAdapterInterceptor(function))
                //设置为0代表 (不进行池化，用完即弃)
                .connectionPool(pool)
                .readTimeout(this.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(this.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(this.timeout.toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .protocols(Lists.newArrayList(Protocol.HTTP_1_1));

        //是否启用代理
        if (Objects.nonNull(proxy)) {
            clientBuilder.proxy(proxy);
        }

        // api 映射
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Optional.ofNullable(url).orElse(BASE_URL))
                .client(clientBuilder.build())
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        this.api = retrofit.create(OpenAiApi.class);
    }


    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }


    public interface GptExecutor {
        /**
         * 处理下载后的文件
         */
        void handle(ChatStreamResult result, boolean isDone) throws Exception;
    }


    /**
     * 普通流式请求
     *
     * @param request  ChatRequest
     * @param executor GptExecutor
     */
    @SneakyThrows
    public void createChatStream(ChatRequest request, GptExecutor executor) {
        createChatStream(request, executor, (Map<String, String>) null);
    }

    @SneakyThrows
    public void createChatStream(ChatRequest request, GptExecutor executor, Map<String, String> headers) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamCallbackHandle callbackHandle = new StreamCallbackHandle(
                countDownLatch, request, executor, instanceName);
        createChatStream(request, headers, callbackHandle);
        // 同步阻塞的方式，执行 executor
        countDownLatch.await(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    public void createChatStream(ChatRequest request, GptExecutor executor, Map<String, String> headers, Map<String, String> queryParams) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamCallbackHandle callbackHandle = new StreamCallbackHandle(
                countDownLatch, request, executor, instanceName);
        createChatStream(request, headers, queryParams, callbackHandle);
        // 同步阻塞的方式，执行 executor
        countDownLatch.await(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
    }


    /**
     * 带函数调用的 createChatStream (Annotation Support)
     *
     * @param request  ChatRequest
     * @param executor GptExecutor
     * @param handlers BaseFunctionHandler...
     */
    public void createChatStream(ChatRequest request, GptExecutor executor, BaseFunctionHandler... handlers) {
        createChatStream(request, executor, null, handlers);
    }

    public void createChatStream(ChatRequest request, GptExecutor executor, Map<String, String> headers, Map<String, String> queryParams, BaseFunctionHandler... handlers) {
        createChatStream(request, executor, headers, queryParams, null, handlers);
    }

    /**
     * 带函数调用的 createChatStream (Annotation Support + ContextHandler)
     *
     * @param request        ChatRequest
     * @param executor       GptExecutor
     * @param contextHandler FunctionContextHandler (External Storage)
     * @param handlers       BaseFunctionHandler...
     */
    public void createChatStream(ChatRequest request, GptExecutor executor, FunctionContextHandler contextHandler, BaseFunctionHandler... handlers) {
        List<AbstractFunHandle> funHandles = new ArrayList<>();
        if (handlers != null) {
            for (BaseFunctionHandler handler : handlers) {
                for (Method method : handler.getClass().getMethods()) {
                    if (method.isAnnotationPresent(AiFunction.class)) {
                        funHandles.add(new AnnotationFunHandle(handler, method));
                    }
                }
            }
        }
        createChatStream(request, executor, contextHandler, funHandles);
    }

    public void createChatStream(ChatRequest request, GptExecutor executor, Map<String, String> headers, Map<String, String> queryParams, FunctionContextHandler contextHandler, BaseFunctionHandler... handlers) {
        List<AbstractFunHandle> funHandles = new ArrayList<>();
        if (handlers != null) {
            for (BaseFunctionHandler handler : handlers) {
                for (Method method : handler.getClass().getMethods()) {
                    if (method.isAnnotationPresent(AiFunction.class)) {
                        funHandles.add(new AnnotationFunHandle(handler, method));
                    }
                }
            }
        }
        createChatStream(request, executor, headers, queryParams, contextHandler, funHandles);
    }


    /**
     * 自动函数调用的 createChatStream
     *
     * @param request    ChatRequest
     * @param executor   GptExecutor
     * @param funHandles AbstractFunHandle
     */
    @SneakyThrows
    public void createChatStream(ChatRequest request, GptExecutor executor, List<AbstractFunHandle> funHandles) {
        createChatStream(request, executor, null, funHandles);
    }

    /**
     * 自动函数调用的 createChatStream (With ContextHandler)
     *
     * @param request        ChatRequest
     * @param executor       GptExecutor
     * @param contextHandler FunctionContextHandler
     * @param funHandles     AbstractFunHandle
     */
    @SneakyThrows
    public void createChatStream(ChatRequest request, GptExecutor executor, FunctionContextHandler contextHandler, List<AbstractFunHandle> funHandles) {
        createChatStream(request, executor, null, null, contextHandler, funHandles);
    }

    @SneakyThrows
    public void createChatStream(ChatRequest request, GptExecutor executor, Map<String, String> headers, Map<String, String> queryParams, FunctionContextHandler contextHandler, List<AbstractFunHandle> funHandles) {
        //填充函数 内容
        if (CollectionUtils.isEmpty(funHandles)) {
            throw new RuntimeException("function handle is empty");
        }

        if (StringUtils.isBlank(request.getToolChoice())) {
            request.setToolChoice("auto");
        }

        //重复函数去重
        funHandles = new ArrayList<>(funHandles.stream()
                .collect(Collectors.toMap(
                        AbstractFunHandle::functionName,   // 使用 functionName 作为键
                        Function.identity(),          // 保留原始对象作为值
                        (existing, replacement) -> existing)) // 如果键重复则保留第一个对象
                .values());

        //组装参数
        request.setTools(funHandles.stream().map(e -> GptTool.builder()
                        .function(GptFunction.builder()
                                .name(e.functionName())
                                .description(e.description())
                                .parameters(e.parametersType())
                                .build())
                        .type("function")
                        .build())
                .collect(Collectors.toList()));

        int maxLoops = 10;
        int loop = 0;

        while (loop < maxLoops) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            StreamCallbackFunctionHandle callbackHandle = new StreamCallbackFunctionHandle(
                    countDownLatch, request, executor, instanceName, funHandles);

            // Pass headers and queryParams
            createChatStream(request, headers, queryParams, callbackHandle);
            // 同步阻塞的方式，执行 executor
            countDownLatch.await(this.timeout.toMillis(), TimeUnit.MILLISECONDS);

            //判断是否触发函数，如果触发，会自动执行函数并更新request，然后continue循环
            if (!this.triggerStreamFunction(request, callbackHandle, funHandles, contextHandler)) {
                break;
            }
            loop++;
        }
    }


    @SneakyThrows
    public void createChatStream(ChatRequest request, Callback<ResponseBody> callback) {
        createChatStream(request, (Map<String, String>) null, callback);
    }

    @SneakyThrows
    public void createChatStream(ChatRequest request, Map<String, String> headers, Callback<ResponseBody> callback) {
        request.setStream(true);
        request.setStreamOptions(new JSONObject().fluentPut("include_usage", true));
//        if (Objects.nonNull(request.getResponseSchemaClass())) {
//            request.setResponseFormat(JsonSchemaTools.responseFormat(request.getResponseSchemaClass()));
//        }
        // sse 流式 handle
        if (headers != null && !headers.isEmpty()) {
            api.createChatStream(request, headers).enqueue(callback);
        } else {
            api.createChatStream(request).enqueue(callback);
        }
    }

    @SneakyThrows
    public void createChatStream(ChatRequest request, Map<String, String> headers, Map<String, String> queryParams, Callback<ResponseBody> callback) {
        request.setStream(true);
        request.setStreamOptions(new JSONObject().fluentPut("include_usage", true));
//        if (Objects.nonNull(request.getResponseSchemaClass())) {
//            request.setResponseFormat(JsonSchemaTools.responseFormat(request.getResponseSchemaClass()));
//        }
        // sse 流式 handle
        Map<String, String> actualHeaders = headers != null ? headers : new HashMap<>();
        if (queryParams != null && !queryParams.isEmpty()) {
            api.createChatStream(request, actualHeaders, queryParams).enqueue(callback);
        } else if (!actualHeaders.isEmpty()) {
            api.createChatStream(request, actualHeaders).enqueue(callback);
        } else {
            api.createChatStream(request).enqueue(callback);
        }
    }


    /**
     * @param request ModerationsRequest
     * @return 参考格式 https://platform.openai.com/docs/api-reference/moderations/object
     */
    public JSONObject moderation(ModerationsRequest request) {
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel());
        labelsParam.put("api", "gpt_moderation");
        labelsParam.put("instanceName", instanceName);
        return CallExecutor.executeWithDot(labelsParam, api.moderations(request),
                mapper);
    }


    @SneakyThrows
    public ChatResult createChat(ChatRequest request) {
        return createChat(request, (Map<String, String>) null);
    }

    @SneakyThrows
    public ChatResult createChat(ChatRequest request, Map<String, String> headers) {
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel().replace(":", "-"));
        labelsParam.put("api", "chat_request");
        labelsParam.put("instanceName", instanceName);

//        if (Objects.nonNull(request.getResponseSchemaClass())) {
//            request.setResponseFormat(JsonSchemaTools.responseFormat(request.getResponseSchemaClass()));
//        }
        ChatResult result = (ChatResult) retryer.call(() -> CallExecutor.executeWithDot(labelsParam,
                (headers != null && !headers.isEmpty()) ? api.createChat(request, headers) : api.createChat(request),
                mapper));
        return result;
    }

    @SneakyThrows
    public ChatResult createChat(ChatRequest request, Map<String, String> headers, Map<String, String> queryParams) {
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel().replace(":", "-"));
        labelsParam.put("api", "chat_request");
        labelsParam.put("instanceName", instanceName);
        ChatResult result = (ChatResult) retryer.call(() -> CallExecutor.executeWithDot(labelsParam,
                (queryParams != null && !queryParams.isEmpty()) 
                        ? api.createChat(request, headers != null ? headers : new HashMap<>(), queryParams) 
                        : ((headers != null && !headers.isEmpty()) ? api.createChat(request, headers) : api.createChat(request)),
                mapper));
        return result;
    }


    /**
     * 带函数调用的 createChat (Annotation Support)
     *
     * @param request  ChatRequest
     * @param handlers BaseFunctionHandler...
     * @return ChatResult
     */
    public ChatResult createChat(ChatRequest request, BaseFunctionHandler... handlers) {
        return createChat(request, null, handlers);
    }

    public ChatResult createChat(ChatRequest request, Map<String, String> headers, Map<String, String> queryParams, BaseFunctionHandler... handlers) {
        return createChat(request, headers, queryParams, null, handlers);
    }

    /**
     * 带函数调用的 createChat (Annotation Support + ContextHandler)
     *
     * @param request        ChatRequest
     * @param contextHandler FunctionContextHandler
     * @param handlers       BaseFunctionHandler...
     * @return ChatResult
     */
    public ChatResult createChat(ChatRequest request, FunctionContextHandler contextHandler, BaseFunctionHandler... handlers) {
        List<AbstractFunHandle> funHandles = new ArrayList<>();
        if (handlers != null) {
            for (BaseFunctionHandler handler : handlers) {
                for (Method method : handler.getClass().getMethods()) {
                    if (method.isAnnotationPresent(AiFunction.class)) {
                        funHandles.add(new AnnotationFunHandle(handler, method));
                    }
                }
            }
        }
        return createChat(request, contextHandler, funHandles);
    }

    public ChatResult createChat(ChatRequest request, Map<String, String> headers, Map<String, String> queryParams, FunctionContextHandler contextHandler, BaseFunctionHandler... handlers) {
        List<AbstractFunHandle> funHandles = new ArrayList<>();
        if (handlers != null) {
            for (BaseFunctionHandler handler : handlers) {
                for (Method method : handler.getClass().getMethods()) {
                    if (method.isAnnotationPresent(AiFunction.class)) {
                        funHandles.add(new AnnotationFunHandle(handler, method));
                    }
                }
            }
        }
        return createChat(request, headers, queryParams, contextHandler, funHandles);
    }


    /**
     * 带函数调用的  createChat
     *
     * @param request    request
     * @param funHandles 期望调用的函数
     * @return ChatResult
     */
    public ChatResult createChat(ChatRequest request, List<AbstractFunHandle> funHandles) {
        return createChat(request, null, funHandles);
    }

    /**
     * 带函数调用的  createChat (With ContextHandler)
     *
     * @param request        request
     * @param contextHandler FunctionContextHandler
     * @param funHandles     期望调用的函数
     * @return ChatResult
     */
        @SneakyThrows
    public ChatResult createChat(ChatRequest request, FunctionContextHandler contextHandler, List<AbstractFunHandle> funHandles) {
        return createChat(request, null, null, contextHandler, funHandles);
    }

    @SneakyThrows
    public ChatResult createChat(ChatRequest request, Map<String, String> headers, Map<String, String> queryParams, FunctionContextHandler contextHandler, List<AbstractFunHandle> funHandles) {
        //填充函数 内容
        if (CollectionUtils.isEmpty(funHandles)) {
            throw new RuntimeException("function handle is empty");
        }

        if (StringUtils.isBlank(request.getToolChoice())) {
            request.setToolChoice("auto");
        }

        //重复函数去重
        funHandles = new ArrayList<>(funHandles.stream()
                .collect(Collectors.toMap(
                        AbstractFunHandle::functionName,   // 使用 functionName 作为键
                        Function.identity(),          // 保留原始对象作为值
                        (existing, replacement) -> existing)) // 如果键重复则保留第一个对象
                .values());

        //组装参数
        request.setTools(funHandles.stream().map(e -> GptTool.builder()
                        .function(GptFunction.builder()
                                .name(e.functionName())
                                .description(e.description())
                                .parameters(e.parametersType())
                                .build())
                        .type("function")
                        .build())
                .collect(Collectors.toList()));

        // Pass headers and queryParams
        ChatResult result = this.createChat(request, headers, queryParams);

        // Loop for automatic function calling
        int maxLoops = 10;
        int loopCount = 0;

        while (this.triggerFunction(request, result, funHandles, contextHandler) && loopCount < maxLoops) {
            loopCount++;
            // Pass headers and queryParams in loop
            result = this.createChat(request, headers, queryParams);
        }

        return result;
    }

    @SneakyThrows
    public ImageResult createImage(ImageRequest request) {
        Map<String, String> labelsParam = new HashMap<>();
        if (DALL_E_3.equals(request.getModel())) {
            request.setQuality("hd");
            labelsParam.put("quality", request.getQuality());
        }
        labelsParam.put("model", request.getModel());
        labelsParam.put("api", "create_image");
        labelsParam.put("instanceName", instanceName);
        return (ImageResult) retryer.call(() -> CallExecutor.executeWithDot(labelsParam,
                api.createImage(request),
                mapper));
    }


    @Deprecated
    public ImageResult editsImage(ImageEditRequest request, File image, File mask) {
        RequestBody imageBody = RequestBody.create(MediaType.parse("image"), image);
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("prompt", request.getPrompt())
                .addFormDataPart("response_format", request.getResponseFormat())
                .addFormDataPart("image", "image", imageBody);

        if (request.getSize() != null) {
            builder.addFormDataPart("size", request.getSize());
        }

        if (request.getN() != null) {
            builder.addFormDataPart("n", request.getN().toString());
        }

        if (request.getModel() != null) {
            builder.addFormDataPart("model", request.getModel());
        }

        if (mask != null) {
            RequestBody maskBody = RequestBody.create(MediaType.parse("image"), mask);
            builder.addFormDataPart("mask", "mask", maskBody);
        }
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel());
        labelsParam.put("api", "edit_image");
        labelsParam.put("instanceName", instanceName);
        return CallExecutor.executeWithDot(
                labelsParam,
                api.imageEdits(builder.build()),
                mapper);
    }


    @SneakyThrows
    public WhisperResult whisper(AudioTextRequest request, File audioFile) {
        RequestBody fileBody = RequestBody.create(MediaType.parse("audio"), audioFile);
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("model", request.getModel())
                .addFormDataPart("file", audioFile.getName(), fileBody);

        if (request.getResponseFormat() != null) {
            builder.addFormDataPart("response_format", request.getResponseFormat());
        }

        if (request.getPrompt() != null) {
            builder.addFormDataPart("prompt", request.getPrompt());
        }

        if (request.getLanguage() != null) {
            builder.addFormDataPart("language", request.getLanguage());
        }

        if (request.getTemperature() != null) {
            builder.addFormDataPart("temperature", request.getTemperature().toString());
        }
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel());
        labelsParam.put("api", "whisper");
        labelsParam.put("instanceName", instanceName);
        WhisperResult result = (WhisperResult) retryer.call(() -> CallExecutor.executeWithDot(labelsParam,
                api.audioText(builder.build()),
                mapper));
        log.info("whisper result:{}", result);
        return result;
    }

    /**
     * openai 文转音
     *
     * @param request
     * @param outPath
     * @return
     */
    @SneakyThrows
    public File speech(SpeechRequest request, String outPath) {
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel());
        labelsParam.put("voice", request.getVoice());
        labelsParam.put("api", "speech");
        labelsParam.put("instanceName", instanceName);

        return (File) retryer.call(() -> {
            try (ResponseBody responseBody = CallExecutor.executeWithDot(
                    labelsParam,
                    api.textToSpeech(request),
                    mapper);
                 InputStream inputStream = responseBody.byteStream();
                 FileOutputStream fos = new FileOutputStream(outPath)) {
                byte[] b = new byte[512];
                int len;
                while ((len = inputStream.read(b)) != -1) {
                    fos.write(b, 0, len);
                }
                return new File(outPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }


    @SneakyThrows
    public EmbeddingResult createEmbeddings(EmbeddingRequest request) {
        Map<String, String> labelsParam = new HashMap<>();
        labelsParam.put("model", request.getModel());
        labelsParam.put("api", "embedding");
        labelsParam.put("instanceName", instanceName);
        return (EmbeddingResult) retryer.call(() -> CallExecutor.executeWithDot(
                labelsParam,
                api.createEmbeddings(request),
                mapper));
    }



    /**
     * 触发普通函数调用
     *
     * @param request        request
     * @param result         result
     * @param funHandles     funHandles
     * @param contextHandler contextHandler
     * @return boolean true if triggered
     */
    private boolean triggerFunction(ChatRequest request, ChatResult result, List<AbstractFunHandle> funHandles, FunctionContextHandler contextHandler) {
        if (!result.triggerFunction()) {
            return false;
        }

        // Add the Assistant message with tool calls to the history before adding tool outputs
        // (Assuming the API requires the assistant's tool_call message in history)
        if (CollectionUtils.isNotEmpty(result.getChoices())) {
            Message assistantMsg = result.getChoices().get(0).getMessage();
            request.addMessage(assistantMsg);

            // Hook: Save Assistant Message
            if (contextHandler != null) {
                contextHandler.onAssistantMessage(assistantMsg);
            }
        }

        // 函数触发 自动调用
        List<GptTool> tools = result.getChoices().stream()
                .map(ChatChoice::getMessage)
                .flatMap(e -> e.getToolCalls().stream()).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(tools)) {
            Map<String, AbstractFunHandle> handleMap = funHandles.stream()
                    .collect(Collectors.toMap(AbstractFunHandle::functionName, e -> e));

            for (GptTool tool : tools) {
                AbstractFunHandle handle = handleMap.get(tool.getFunction().getName());
                if (handle != null) {
                    handle.handle(JSON.parseObject(tool.getFunction().getArguments()), request, tool.getId());
                }
            }

            // Hook: Save Tool Messages
            if (contextHandler != null && CollectionUtils.isNotEmpty(request.getMessages())) {
                List<BaseMessage> allMessages = request.getMessages();
                int toolCount = tools.size();
                int totalSize = allMessages.size();
                // Ensure we don't go out of bounds
                for (int i = Math.max(0, totalSize - toolCount); i < totalSize; i++) {
                    if (allMessages.get(i) instanceof Message) {
                        Message msg = (Message) allMessages.get(i);
                        if (MessageRole.tool.role.equals(msg.getRole())) {
                            contextHandler.onToolMessage(msg);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 触发流式函数调用
     *
     * @param request        ChatRequest
     * @param callbackHandle StreamCallbackFunctionHandle
     * @param funHandles     funHandles
     * @param contextHandler contextHandler
     * @return boolean True if function was triggered and handled, False otherwise
     */
    private boolean triggerStreamFunction(ChatRequest request, StreamCallbackFunctionHandle callbackHandle, List<AbstractFunHandle> funHandles, FunctionContextHandler contextHandler) {
        // 函数触发 自动调用
        List<FunctionTrigger> functionArguments = callbackHandle.context.getFunctionArguments();
        if (CollectionUtils.isNotEmpty(functionArguments)) {

            // Build assistant message with tool calls
            List<GptTool> toolCalls = new ArrayList<>();
            for (FunctionTrigger trigger : functionArguments) {
                toolCalls.add(GptTool.builder()
                        .id(trigger.getId())
                        .type("function")
                        .function(GptFunction.builder()
                                .name(trigger.getName())
                                .arguments(trigger.getArguments().toString())
                                .build())
                        .build());
            }

            Message assistantMessage = Message.builder()
                    .role(MessageRole.assistant.role)
                    .content(callbackHandle.responseStream.length() > 0 ? callbackHandle.responseStream.toString() : null)
                    .reasoningContent(callbackHandle.reasoningStream.length() > 0 ? callbackHandle.reasoningStream.toString() : null)
                    .toolCalls(toolCalls)
                    .build();
            request.addMessage(assistantMessage);

            // Hook: Save Assistant Message
            if (contextHandler != null) {
                contextHandler.onAssistantMessage(assistantMessage);
            }

            Map<String, AbstractFunHandle> handleMap = funHandles.stream()
                    .collect(Collectors.toMap(AbstractFunHandle::functionName, e -> e));

            for (FunctionTrigger tool : functionArguments) {
                AbstractFunHandle handle = handleMap.get(tool.getName());
                if (handle != null) {
                    handle.handle(JSON.parseObject(tool.getArguments().toString()), request, tool.getId());
                }
            }

            // Hook: Save Tool Messages
            if (contextHandler != null && CollectionUtils.isNotEmpty(request.getMessages())) {
                List<BaseMessage> allMessages = request.getMessages();
                int toolCount = functionArguments.size();
                int totalSize = allMessages.size();
                for (int i = Math.max(0, totalSize - toolCount); i < totalSize; i++) {
                    if (allMessages.get(i) instanceof Message) {
                        Message msg = (Message) allMessages.get(i);
                        if (MessageRole.tool.role.equals(msg.getRole())) {
                            contextHandler.onToolMessage(msg);
                        }
                    }

                }
            }

            return true;
        }
        return false;
    }

}
