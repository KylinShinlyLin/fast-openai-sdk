package com.kylin.fast.openai;

import com.alibaba.fastjson.JSON;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.function.FunctionContextHandler;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;
import com.kylin.fast.openai.function.handler.BaseFunctionHandler;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.result.ChatResult;
import com.kylin.fast.openai.result.dto.ChatCompletionStreamChoice;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class OpenAiFunctionCallTest {

    private static OpenAiService service;

    @BeforeAll
    public static void init() {
        System.out.println("init function call test...");
        // 从 src/main/resources/fast-openai.properties 自动读取配置，包含 apikey、proxy 等
        OpenAiConfig config = OpenAiConfig.loadFromProperties("fast-openai.properties");
        service = new OpenAiService(config);
    }

    /**
     * 自定义函数处理器，实现 BaseFunctionHandler 接口
     */
    public static class WeatherFunctionHandler implements BaseFunctionHandler {

        @AiFunction(name = "get_current_weather", description = "获取指定地点的当前天气情况")
        public String getCurrentWeather(
                @AiFunctionParam(name = "location", description = "城市名称，例如：北京、上海") String location,
                @AiFunctionParam(name = "unit", description = "温度单位，例如：celsius 或 fahrenheit", required = false) String unit
        ) {
            System.out.println("\n================================");
            System.out.println("----> AI 自动触发调用了本地函数 get_current_weather!");
            System.out.println("----> 请求地点: " + location + ", 单位: " + unit);
            System.out.println("================================\n");

            // 模拟根据地点返回天气
            if (location.contains("北京")) {
                return "北京，25 摄氏度。";
            } else if (location.contains("上海")) {
                return "上海， 22 摄氏度。";
            }
            return location + "的天气是阴天，温度 20 摄氏度。";
        }
    }

    /**
     * 测试使用注解方式进行 Function Call (自动函数调用 - 非流式)
     */
    @Test
    public void testFunctionCall() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o-mini") // 函数调用通常需要支持 function_call 的模型
                .addMessage(Message.of(MessageRole.user, "请问北京和上海今天的天气怎么样？分别告诉我。markdown 返回"))
                .build();

        // 传入带有 @AiFunction 注解的 handler，SDK 会自动解析注解并自动将结果喂回给大模型，最终返回大模型润色后的回答
        ChatResult result = service.createChat(request, new WeatherFunctionHandler());

        System.out.println("\n--- 最终模型给出的回答 ---");
        System.out.println(result.getChoices().get(0).getMessage().getContent());

        if (result.getUsage() != null) {
            System.out.println("\nUsage: " + result.getUsage());
        }
    }

    /**
     * 测试使用注解方式进行 Function Call (自动函数调用 - 流式)
     */
    @Test
    public void testFunctionCallStream() throws InterruptedException {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o-mini") // 函数调用通常需要支持 function_call 的模型
                .addMessage(Message.of(MessageRole.user, "请问北京今天的天气怎么样？"))
                .build();

        System.out.println("--- 流式回答开始 ---");
        // 调用流式的 API 接口，同样传入 new WeatherFunctionHandler()，SDK 会自动处理函数调用的挂起和恢复，最终将润色后的回答以流式抛出
        service.createChatStream(request, (result, isDone) -> {
            if (Objects.nonNull(result.getUsage())) {
                System.out.printf("usage=%s%n", result.getUsage());
            }
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                if (choice.getDelta().getContent() != null) {
                    System.out.print(choice.getDelta().getContent());
                    System.out.flush();
                }
            }
        }, new WeatherFunctionHandler());

        // 等待流式输出完成
        Thread.sleep(8000);
        System.out.println("\n--- 流式回答结束 ---");
    }

    /**
     * 测试 FunctionContextHandler 记录上下文 (非流式)
     */
    @Test
    public void testFunctionCallWithContextHandler() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(Message.of(MessageRole.user, "上海今天的天气怎么样？"))
                .build();

        FunctionContextHandler contextHandler = new FunctionContextHandler() {
            @Override
            public void onAssistantMessage(Message message) {
                System.out.println("\n[Context Hook - 非流式] 保存 Assistant ToolCall 消息:");
                System.out.println(JSON.toJSONString(message));
            }

            @Override
            public void onToolMessage(Message message) {
                System.out.println("\n[Context Hook - 非流式] 保存 Tool Result 消息:");
                System.out.println(JSON.toJSONString(message));
            }
        };

        System.out.println("--- 非流式 (带 ContextHandler) 回答开始 ---");
        ChatResult result = service.createChat(request, contextHandler, new WeatherFunctionHandler());

        System.out.println("\n--- 最终模型给出的回答 ---");
        System.out.println(result.getChoices().get(0).getMessage().getContent());
    }

    /**
     * 测试 FunctionContextHandler 记录上下文 (流式)
     */
    @Test
    public void testFunctionCallStreamWithContextHandler() throws InterruptedException {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(Message.of(MessageRole.user, "北京和上海今天的天气怎么样？顺便给我写 1000字有关全球变暖的论文"))
                .build();

        FunctionContextHandler contextHandler = new FunctionContextHandler() {
            @Override
            public void onAssistantMessage(Message message) {
                System.out.println("\n[Context Hook - 流式] 保存 Assistant ToolCall 消息:");
                System.out.println(JSON.toJSONString(message));
            }

            @Override
            public void onToolMessage(Message message) {
                System.out.println("\n[Context Hook - 流式] 保存 Tool Result 消息:");
                System.out.println(JSON.toJSONString(message));
            }
        };

        System.out.println("--- 流式 (带 ContextHandler) 回答开始 ---");
        service.createChatStream(request, (result, isDone) -> {
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                if (choice.getDelta().getContent() != null) {
                    System.out.print(choice.getDelta().getContent());
                    System.out.flush();
                }
            }
        }, contextHandler, new WeatherFunctionHandler());

        // 等待流式输出完成
        Thread.sleep(8000);
        System.out.println("\n--- 流式回答结束 ---");
    }
}
