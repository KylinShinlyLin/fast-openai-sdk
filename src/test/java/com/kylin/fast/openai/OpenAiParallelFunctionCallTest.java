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

/**
 * 并行函数调用 (Parallel Function Calling) 测试类
 * 
 * 演示 OpenAI 的并行函数调用能力：AI 可以在一次响应中同时请求调用多个函数，
 * SDK 会并行执行这些函数调用，然后将结果统一返回给 AI 生成最终回答。
 */
public class OpenAiParallelFunctionCallTest {

    private static OpenAiService service;

    @BeforeAll
    public static void init() {
        System.out.println("init parallel function call test...");
        // 从 src/main/resources/fast-openai.properties 自动读取配置，包含 apikey、proxy 等
        OpenAiConfig config = OpenAiConfig.loadFromProperties("fast-openai.properties");
        service = new OpenAiService(config);
    }

    /**
     * 并行函数调用处理器 - 包含多个工具函数
     * 用于演示 AI 一次性并行调用多个函数的场景
     */
    public static class ParallelFunctionHandler implements BaseFunctionHandler {

        @AiFunction(name = "get_current_weather", description = "获取指定地点的当前天气情况")
        public String getCurrentWeather(
                @AiFunctionParam(name = "location", description = "城市名称，例如：北京、上海、广州") String location,
                @AiFunctionParam(name = "unit", description = "温度单位，例如：celsius 或 fahrenheit", required = false) String unit
        ) {
            System.out.println("\n[并行调用] 触发函数: get_current_weather, 地点: " + location);
            // 模拟不同城市的天气
            switch (location) {
                case "北京":
                    return "北京今天晴天，25°C，空气质量良";
                case "上海":
                    return "上海今天多云，22°C，湿度65%";
                case "广州":
                    return "广州今天小雨，28°C，空气质量优";
                case "深圳":
                    return "深圳今天阴天，26°C，微风";
                default:
                    return location + "今天晴转多云，20°C";
            }
        }

        @AiFunction(name = "get_current_time", description = "获取指定地点的当前时间")
        public String getCurrentTime(
                @AiFunctionParam(name = "timezone", description = "时区，例如：Asia/Shanghai、Asia/Tokyo、America/New_York") String timezone
        ) {
            System.out.println("\n[并行调用] 触发函数: get_current_time, 时区: " + timezone);
            // 模拟不同时区的时间
            switch (timezone) {
                case "Asia/Shanghai":
                    return "北京时间: 2024年1月15日 14:30";
                case "Asia/Tokyo":
                    return "东京时间: 2024年1月15日 15:30";
                case "America/New_York":
                    return "纽约时间: 2024年1月15日 01:30";
                case "Europe/London":
                    return "伦敦时间: 2024年1月15日 06:30";
                default:
                    return timezone + "当前时间: 2024年1月15日 12:00";
            }
        }

        @AiFunction(name = "calculate", description = "执行数学计算")
        public String calculate(
                @AiFunctionParam(name = "expression", description = "数学表达式，例如：23 + 45、100 * 0.15、(50 - 20) / 2") String expression
        ) {
            System.out.println("\n[并行调用] 触发函数: calculate, 表达式: " + expression);
            // 简单模拟计算结果
            if (expression.contains("+")) {
                return "计算结果: " + expression + " = 68";
            } else if (expression.contains("*")) {
                return "计算结果: " + expression + " = 15";
            } else if (expression.contains("/")) {
                return "计算结果: " + expression + " = 15";
            }
            return "计算结果: " + expression + " = 0";
        }

        @AiFunction(name = "get_stock_price", description = "获取指定股票的当前价格")
        public String getStockPrice(
                @AiFunctionParam(name = "symbol", description = "股票代码，例如：AAPL、TSLA、MSFT、BABA") String symbol
        ) {
            System.out.println("\n[并行调用] 触发函数: get_stock_price, 股票: " + symbol);
            // 模拟股票价格
            switch (symbol.toUpperCase()) {
                case "AAPL":
                    return "苹果(AAPL)当前股价: $185.50, 涨幅: +1.2%";
                case "TSLA":
                    return "特斯拉(TSLA)当前股价: $245.30, 涨幅: +2.5%";
                case "MSFT":
                    return "微软(MSFT)当前股价: $420.15, 涨幅: +0.8%";
                case "BABA":
                    return "阿里巴巴(BABA)当前股价: $78.90, 跌幅: -0.5%";
                default:
                    return symbol + "当前股价: $100.00, 持平";
            }
        }
    }

    /**
     * 测试并行函数调用 - 非流式 (Parallel Function Calling)
     * AI 会在一次响应中同时调用多个函数，然后 SDK 并行执行这些函数调用
     */
    @Test
    public void testParallelFunctionCall() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4-mini")
                .addMessage(Message.of(MessageRole.user, 
                    "请帮我查询北京、上海和广州今天的天气，同时告诉我北京、东京和纽约的当前时间，" +
                    "再计算一下 23+45 和 100*0.15 的结果。请用表格形式整理所有信息。"))
                .build();

        System.out.println("--- 并行函数调用测试开始 (非流式) ---");
        long startTime = System.currentTimeMillis();
        
        ChatResult result = service.createChat(request, new ParallelFunctionHandler());

        long endTime = System.currentTimeMillis();
        System.out.println("\n--- 最终模型给出的回答 ---");
        System.out.println(result.getChoices().get(0).getMessage().getContent());
        System.out.println("\n--- 执行耗时: " + (endTime - startTime) + "ms ---");
        
        if (result.getUsage() != null) {
            System.out.println("Token Usage: " + result.getUsage());
        }
    }

    /**
     * 测试并行函数调用 - 流式 (Parallel Function Calling with Stream)
     * 展示流式输出时如何处理并行函数调用
     */
    @Test
    public void testParallelFunctionCallStream() throws InterruptedException {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4-mini")
                .addMessage(Message.of(MessageRole.user, 
                    "帮我查一下AAPL、TSLA和MSFT的股票价格，同时告诉我上海和伦敦的时间，" +
                    "再计算 (50-20)/2 的结果。"))
                .parallelToolCalls(true)
                .build();

        System.out.println("--- 并行函数调用测试开始 (流式) ---");
        long startTime = System.currentTimeMillis();
        
        service.createChatStream(request, (result, isDone) -> {
            if (Objects.nonNull(result.getUsage())) {
                System.out.printf("\n[Usage] %s%n", result.getUsage());
            }
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                if (choice.getDelta().getContent() != null) {
                    System.out.print(choice.getDelta().getContent());
                    System.out.flush();
                }
            }
        }, new ParallelFunctionHandler());

        // 等待流式输出完成
        Thread.sleep(10000);
        
        long endTime = System.currentTimeMillis();
        System.out.println("\n\n--- 流式执行耗时: " + (endTime - startTime) + "ms ---");
    }

    /**
     * 测试复杂场景下的并行函数调用 - 非流式
     * 结合天气、时间和股票等多个数据源
     */
    @Test
    public void testComplexParallelFunctionCall() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4-mini")
                .addMessage(Message.of(MessageRole.user, 
                    "我打算出差，需要了解以下信息：\n" +
                    "1. 北京和深圳今天的天气\n" +
                    "2. 北京和纽约现在的时间\n" +
                    "3. AAPL和BABA的股价\n" +
                    "4. 计算如果我有10000元，以BABA当前价格能买多少股（假设价格为78.9美元，汇率7.2）\n" +
                    "请整理成一份出差简报。"))
                .parallelToolCalls(true)
                .build();

        System.out.println("--- 复杂并行函数调用测试开始 ---");
        long startTime = System.currentTimeMillis();
        
        ChatResult result = service.createChat(request, new ParallelFunctionHandler());

        long endTime = System.currentTimeMillis();
        System.out.println("\n--- 最终模型给出的出差简报 ---");
        System.out.println(result.getChoices().get(0).getMessage().getContent());
        System.out.println("\n--- 执行耗时: " + (endTime - startTime) + "ms ---");
        
        if (result.getUsage() != null) {
            System.out.println("Token Usage: " + result.getUsage());
        }
    }

    /**
     * 测试带上下文的并行函数调用 - 流式
     * 使用 FunctionContextHandler 记录并行调用的上下文
     */
    @Test
    public void testParallelFunctionCallStreamWithContext() throws InterruptedException {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4-mini")
                .addMessage(Message.of(MessageRole.user, 
                    "同时查询上海天气、东京时间、TSLA股价，并计算 50*3 的结果。"))
                .parallelToolCalls(true)
                .build();

        FunctionContextHandler contextHandler = new FunctionContextHandler() {
            private int assistantCount = 0;
            private int toolCount = 0;

            @Override
            public void onAssistantMessage(Message message) {
                assistantCount++;
                System.out.println("\n[Context Hook] 第" + assistantCount + "个 Assistant ToolCall 消息:");
                System.out.println(JSON.toJSONString(message));
            }

            @Override
            public void onToolMessage(Message message) {
                toolCount++;
                System.out.println("\n[Context Hook] 第" + toolCount + "个 Tool Result 消息:");
                System.out.println(JSON.toJSONString(message));
            }
        };

        System.out.println("--- 带上下文的并行函数调用测试开始 (流式) ---");
        
        service.createChatStream(request, (result, isDone) -> {
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                if (choice.getDelta().getContent() != null) {
                    System.err.print(choice.getDelta().getContent());
                    System.err.flush();
                }
            }
        }, contextHandler, new ParallelFunctionHandler());

        // 等待流式输出完成
//        Thread.sleep(10000);
//        System.out.println("\n\n--- 测试结束 ---");
    }
}
