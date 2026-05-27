package com.kylin.fast.graph.core;

import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.graph.model.ToolCall;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.BaseMessage;
import com.kylin.fast.openai.request.dto.GptFunction;
import com.kylin.fast.openai.request.dto.GptTool;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.result.ChatResult;
import com.kylin.fast.openai.result.dto.ChatChoice;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🎯 Graph 工具调用完整 Demo
 *
 * 演示 ReAct (Reasoning + Acting) 模式：
 *   用户提问 → Agent 节点 (LLM 决策) → [有工具调用?] → Action 节点 (并发执行工具) → Agent 节点 → ... → 最终回复
 *
 * <pre>
 *   ┌─────────┐     有 ToolCalls     ┌──────────┐
 *   │  agent  │ ─────────────────→  │  action   │
 *   │ (LLM)   │ ←─────────────────  │ (工具执行) │
 *   └─────────┘     执行完成后返回    └──────────┘
 *        │
 *        │ 无 ToolCalls (最终回复)
 *        ▼
 *     __end__
 * </pre>
 *
 * 运行前请设置环境变量 OPENAI_API_KEY，或者修改 init() 中的 API Key。
 *
 * @author AI Agent
 */
public class GraphToolCallingDemo {

    // ==================== 1. 定义 Agent 状态 ====================
    /**
     * Agent 的状态对象 —— 在整个 ReAct 循环中流转。
     * 必须包含 getMessages() 方法供 OpenAiAgentNode 和 OpenAiActionNode 反射读取。
     */
    public static class AgentState {
        private List<ChatMessage> messages = new ArrayList<>();

        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    }

    // ==================== 2. 定义工具 (使用 @AiFunction 注解) ====================
    /**
     * 工具类 —— 用 @AiFunction 注解标记的方法会自动被 CompiledGraph.registerTools() 解析，
     * 生成 GptTool 元数据发给大模型，并在大模型返回 tool_calls 时自动反射调用。
     */
    public static class MyTools {

        @AiFunction(name = "calculator", description = "执行基本的数学运算：加法、减法、乘法、除法")
        public static double calculator(
                @AiFunctionParam(name = "a", description = "第一个数字") double a,
                @AiFunctionParam(name = "b", description = "第二个数字") double b,
                @AiFunctionParam(name = "operation", description = "运算符：add, subtract, multiply, divide") String operation
        ) {
            System.out.println(String.format("  🧮 [calculator] %s %s %s", a, operation, b));
            switch (operation) {
                case "add":      return a + b;
                case "subtract": return a - b;
                case "multiply": return a * b;
                case "divide":
                    if (b == 0) throw new IllegalArgumentException("除数不能为零");
                    return a / b;
                default:
                    throw new IllegalArgumentException("不支持的运算: " + operation);
            }
        }

        @AiFunction(name = "get_current_weather", description = "获取指定城市的当前天气情况")
        public static String getCurrentWeather(
                @AiFunctionParam(name = "location", description = "城市名称，例如：北京、上海、东京") String location,
                @AiFunctionParam(name = "unit", description = "温度单位，celsius(摄氏度) 或 fahrenheit(华氏度)", defaultValue = "celsius") String unit
        ) {
            System.out.println(String.format("  🌤️ [get_current_weather] location=%s, unit=%s", location, unit));
            // 模拟天气数据
            Map<String, String> weatherDB = new HashMap<>();
            weatherDB.put("北京", "晴天，气温 25°C，湿度 40%，风力 2 级");
            weatherDB.put("上海", "阴转小雨，气温 22°C，湿度 75%，风力 3 级");
            weatherDB.put("深圳", "多云，气温 28°C，湿度 65%，风力 1 级");
            weatherDB.put("东京", "晴转多云，气温 18°C，湿度 50%，风力 2 级");
            weatherDB.put("纽约", "小雪，气温 -2°C，湿度 60%，风力 4 级");
            return weatherDB.getOrDefault(location, location + " 目前天气：多云，气温 20°C，湿度 55%");
        }

        @AiFunction(name = "search_knowledge", description = "在知识库中搜索信息，返回匹配的内容摘要")
        public static String searchKnowledge(
                @AiFunctionParam(name = "query", description = "搜索关键词") String query,
                @AiFunctionParam(name = "max_results", description = "最大返回数量", defaultValue = "3") int maxResults
        ) {
            System.out.println(String.format("  🔍 [search_knowledge] query=%s, max=%d", query, maxResults));
            // 模拟知识库
            Map<String, String> knowledgeDB = new HashMap<>();
            knowledgeDB.put("java", "Java 是一门面向对象的编程语言，广泛用于企业级应用开发。JDK 8 引入了 Lambda 表达式和 Stream API。");
            knowledgeDB.put("python", "Python 是一门解释型高级编程语言，以简洁易读著称，在 AI 和数据科学领域非常流行。");
            knowledgeDB.put("graph", "StateGraph 是一个轻量级的有向状态图引擎，支持节点编排、条件路由、状态合并和 Checkpoint 持久化。");
            knowledgeDB.put("react", "ReAct (Reasoning + Acting) 是一种 AI Agent 模式，交替进行推理和行动，通过工具调用与外部环境交互。");
            knowledgeDB.put("openai", "OpenAI 是一家人工智能研究公司，开发了 GPT 系列大语言模型，提供 Chat Completions API 接口。");

            return knowledgeDB.entrySet().stream()
                    .filter(e -> e.getKey().contains(query.toLowerCase()) || e.getValue().contains(query))
                    .limit(maxResults)
                    .map(e -> "📄 " + e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
        }
    }

    // ==================== 3. 构建并编译 Graph ====================
    private static CompiledGraph<AgentState> compiled;
    private static OpenAiService service;

    @BeforeAll
    public static void init() {
        // ---------- 3.1 初始化 OpenAiService ----------
        // 🔑 请通过环境变量 OPENAI_API_KEY 传入你的 API Key，或直接替换下面的字符串
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getProperty("OPENAI_API_KEY", "your-api-key-here");
        }
        service = new OpenAiService(
                Collections.singletonList(apiKey),
                Duration.ofSeconds(120)
        );
        System.out.println("✅ OpenAiService 初始化完成");

        // ---------- 3.2 构建 StateGraph ----------
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

        // 注册 Reducer：messages 字段使用追加模式（而不是覆盖）
        graph.registerReducer("messages", (oldVal, newVal) -> {
            List<ChatMessage> merged = new ArrayList<>();
            if (oldVal != null) merged.addAll((Collection<ChatMessage>) oldVal);
            if (newVal != null) merged.addAll((Collection<ChatMessage>) newVal);
            return merged;
        });

        // ---------- 3.3 编译图（此时先不注册工具，等 compile 后再注册） ----------
        compiled = graph.compile(new InMemorySaver())
                .registerTools(new MyTools());  // 🌟 一行代码注册所有 @AiFunction 工具！

        System.out.println("✅ 已注册工具: " + compiled.getGptTools().stream()
                .map(t -> t.getFunction().getName())
                .collect(Collectors.toList()));

        // ---------- 3.4 添加 Agent 节点（LLM 决策） ----------
        graph.addNode("agent", state -> {
            System.out.println("\n🤖 [Agent Node] 正在调用大模型进行决策...");

            // 将 ChatMessage 转为 SDK 的 BaseMessage
            List<BaseMessage> sdkMessages = new ArrayList<>();
            for (ChatMessage m : state.getMessages()) {
                Message sdkMsg = new Message();
                sdkMsg.setRole(m.getRole());
                sdkMsg.setContent(m.getContent());
                sdkMsg.setName(m.getName());
                sdkMsg.setToolCallId(m.getToolCallId());
                // 传递 tool_calls（上轮 LLM 返回的工具调用信息）
                if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                    List<GptTool> tcs = new ArrayList<>();
                    for (ToolCall tc : m.getToolCalls()) {
                        tcs.add(GptTool.builder()
                                .id(tc.getId())
                                .function(GptFunction.builder()
                                        .name(tc.getName())
                                        .arguments(tc.getArguments())
                                        .build())
                                .type("function").build());
                    }
                    sdkMsg.setToolCalls(tcs);
                }
                sdkMessages.add(sdkMsg);
            }

            // 构建请求并注入已注册的工具声明
            ChatRequest request = ChatRequest.builder()
                    .model("gpt-4.1-mini")  // 可替换为 gpt-4o-mini, gpt-4.1-nano 等
                    .messages(sdkMessages)
                    .build();
            request.setTools(compiled.getGptTools());  // 🌟 自动注入所有工具描述

            // 调用大模型
            ChatResult result = service.createChat(request);
            Map<String, Object> update = new HashMap<>();

            if (result.triggerFunction()) {
                // 模型决定调用工具
                List<ToolCall> toolCalls = new ArrayList<>();
                for (ChatChoice choice : result.getChoices()) {
                    if (choice.getMessage() != null && choice.getMessage().getToolCalls() != null) {
                        for (GptTool gt : choice.getMessage().getToolCalls()) {
                            if (gt.getFunction() != null) {
                                ToolCall tc = new ToolCall(
                                        gt.getId(),
                                        gt.getFunction().getName(),
                                        gt.getFunction().getArguments()
                                );
                                toolCalls.add(tc);
                                System.out.println("  📞 模型决定调用工具: " + tc.getName() + "(" + tc.getArguments() + ")");
                            }
                        }
                    }
                }
                update.put("messages", Collections.singletonList(
                        ChatMessage.assistant("正在调用工具...", toolCalls)));
            } else {
                // 模型给出最终回复
                String reply = result.content();
                System.out.println("  💬 模型最终回复: " + reply);
                update.put("messages", Collections.singletonList(
                        ChatMessage.assistant(reply)));
            }
            return update;
        });

        // ---------- 3.5 添加 Action 节点（工具并发执行） ----------
        // 🌟 使用内置的 OpenAiActionNode，自动并行执行工具！
        graph.addNode("action", new OpenAiActionNode<>(compiled));

        // ---------- 3.6 连线 ----------
        graph.addEdge(StateGraph.START, "agent");

        // 条件边：agent 之后 → 有 tool_calls 去 action，否则结束
        graph.addConditionalEdges("agent", state -> {
            List<ChatMessage> history = state.getMessages();
            if (history.isEmpty()) return "end";
            ChatMessage last = history.get(history.size() - 1);
            if (last.getToolCalls() != null && !last.getToolCalls().isEmpty()) {
                return "action";
            }
            return "end";
        }, new HashMap<String, String>() {{
            put("action", "action");
            put("end", StateGraph.END);
        }});

        // action 之后回到 agent 继续决策
        graph.addEdge("action", "agent");

        System.out.println("✅ Graph 编译完成！节点: " + graph.getNodes().keySet());
    }

    // ==================== 4. 测试用例 ====================

    /**
     * 🧪 Demo 1: 数学计算 —— 触发 calculator 工具
     */
    @Test
    public void testCalculatorTool() {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("🧪 Demo 1: 数学计算工具调用");
        System.out.println("══════════════════════════════════════════");

        AgentState initial = new AgentState();
        initial.getMessages().add(ChatMessage.user("请帮我计算 156 乘以 38 等于多少？"));

        AgentState finalState = compiled.invoke(
                initial,
                GraphConfig.builder().threadId("demo-calc-01").build()
        );

        printConversation(finalState);
        // 验证最后一条消息是 assistant 的最终回复
        ChatMessage lastMsg = finalState.getMessages().get(finalState.getMessages().size() - 1);
        assertEquals("assistant", lastMsg.getRole());
        assertTrue(lastMsg.getContent().contains("5928"),
                "期望计算结果包含 5928，实际: " + lastMsg.getContent());
    }

    /**
     * 🧪 Demo 2: 天气查询 —— 触发 get_current_weather 工具
     */
    @Test
    public void testWeatherTool() {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("🧪 Demo 2: 天气查询工具调用");
        System.out.println("══════════════════════════════════════════");

        AgentState initial = new AgentState();
        initial.getMessages().add(ChatMessage.user("北京和上海今天天气怎么样？"));

        AgentState finalState = compiled.invoke(
                initial,
                GraphConfig.builder().threadId("demo-weather-01").build()
        );

        printConversation(finalState);
        ChatMessage lastMsg = finalState.getMessages().get(finalState.getMessages().size() - 1);
        assertEquals("assistant", lastMsg.getRole());
    }

    /**
     * 🧪 Demo 3: 知识搜索 + 综合推理 —— 触发 search_knowledge + 多轮 ReAct
     */
    @Test
    public void testKnowledgeSearch() {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("🧪 Demo 3: 知识搜索工具调用");
        System.out.println("══════════════════════════════════════════");

        AgentState initial = new AgentState();
        initial.getMessages().add(ChatMessage.user("什么是 ReAct Agent 模式？它和传统的对话模型有什么区别？"));

        AgentState finalState = compiled.invoke(
                initial,
                GraphConfig.builder().threadId("demo-knowledge-01").build()
        );

        printConversation(finalState);
        ChatMessage lastMsg = finalState.getMessages().get(finalState.getMessages().size() - 1);
        assertEquals("assistant", lastMsg.getRole());
    }

    /**
     * 🧪 Demo 4: 流式调用 —— 实时查看 ReAct 循环的每一步
     */
    @Test
    public void testStreamMode() {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("🧪 Demo 4: 流式模式 —— 实时观察 ReAct 循环");
        System.out.println("══════════════════════════════════════════");

        AgentState initial = new AgentState();
        initial.getMessages().add(ChatMessage.system("你是一个有用的助手，请用中文回答。"));
        initial.getMessages().add(ChatMessage.user("帮我查一下深圳的天气，然后告诉我今天适合出门吗？"));

        Iterator<GraphEvent> stream = compiled.stream(
                initial,
                GraphConfig.builder().threadId("demo-stream-01").build()
        );

        int stepCount = 0;
        while (stream.hasNext()) {
            GraphEvent event = stream.next();
            switch (event.getType()) {
                case "node_start":
                    stepCount++;
                    System.out.println(String.format("\n▶️  Step %d: 进入节点 [%s]", stepCount, event.getNodeName()));
                    break;
                case "node_end":
                    System.out.println(String.format("✅ Step %d: 节点 [%s] 执行完成", stepCount, event.getNodeName()));
                    break;
                case "token":
                    // 透传 token（如果有的话）
                    System.out.print(event.getPayload());
                    break;
                case "error":
                    System.err.println("❌ 错误: " + event.getPayload());
                    break;
            }
        }
        System.out.println("\n\n🎉 ReAct 循环完成！共执行 " + stepCount + " 步");
        assertTrue(stepCount >= 2, "至少应该执行 agent → action → agent (3步)");
    }

    // ==================== 辅助方法 ====================
    private void printConversation(AgentState state) {
        System.out.println("\n📋 完整对话历史:");
        System.out.println("──────────────────────────────────────────");
        for (ChatMessage m : state.getMessages()) {
            String tools = m.getToolCalls() != null && !m.getToolCalls().isEmpty()
                    ? " 🔧" + m.getToolCalls().stream().map(ToolCall::getName).collect(Collectors.toList())
                    : "";
            String preview = m.getContent() != null && m.getContent().length() > 120
                    ? m.getContent().substring(0, 120) + "..."
                    : m.getContent();
            System.out.println(String.format("  [%s]%s: %s", m.getRole(), tools, preview));
        }
        System.out.println("──────────────────────────────────────────\n");
    }
}
