package com.kylin.fast.openai;

import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;
import com.kylin.fast.openai.function.handler.BaseFunctionHandler;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.result.ChatResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
            System.out.println("================================");
            System.out.println("----> AI 自动触发调用了本地函数 get_current_weather!");
            System.out.println("----> 请求地点: " + location + ", 单位: " + unit);
            System.out.println("================================");

            // 模拟根据地点返回天气
            if (location.contains("北京")) {
                return "北京的天气是晴天，温度 25 摄氏度。";
            } else if (location.contains("上海")) {
                return "上海的天气是多云，温度 22 摄氏度。";
            }
            return location + "的天气是阴天，温度 20 摄氏度。";
        }
    }

    /**
     * 测试使用注解方式进行 Function Call (自动函数调用)
     */
    @Test
    public void testFunctionCall() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o-mini") // 函数调用通常需要支持 function_call 的模型
                .addMessage(Message.of(MessageRole.user, "请问北京和上海今天的天气怎么样？分别告诉我。"))
                .build();

        // 传入带有 @AiFunction 注解的 handler，SDK 会自动解析注解并自动将结果喂回给大模型，最终返回大模型润色后的回答
        ChatResult result = service.createChat(request, new WeatherFunctionHandler());

        System.out.println("\n--- 最终模型给出的回答 ---");
        System.out.println(result.getChoices().get(0).getMessage().getContent());

        if (result.getUsage() != null) {
            System.out.println("\nUsage: " + result.getUsage());
        }
    }
}
