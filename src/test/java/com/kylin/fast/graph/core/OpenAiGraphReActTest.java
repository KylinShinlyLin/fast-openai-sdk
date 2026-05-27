package com.kylin.fast.graph.core;

import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🌟 [生产级终极黄金模板] 极简并发 ReAct Agent 状态图测试
 * 
 * 通过将 [Agent Node] 封装为 {@link OpenAiAgentNode}，将 [Action Node] 封装为 {@link OpenAiActionNode}，
 * 使得整套复杂、高并发的 ReAct 智能体编排，缩减到了两行核心代码！
 * 同时保留了自定义的高保真反射、多线程 parallelStream 并行与 100% 状态隔离。
 * 
 * <pre>
 *   ┌─────────┐     有 ToolCalls     ┌──────────┐
 *   │  agent  │ ─────────────────→  │  action   │
 *   │ (LLM)   │ ←─────────────────  │ (并发工具) │
 *   └─────────┘     执行完成后返回    └──────────┘
 *        │
 *        │ 无 ToolCalls (最终回复)
 *        ▼
 *     __end__
 * </pre>
 * 
 * 参考: OpenAiGraphTest4 (openai-api 项目)
 * 
 * @author AI Agent
 */
public class OpenAiGraphReActTest {

    private static OpenAiService service;
    private static CompiledGraph<State> compiled;

    // ==================== 1. 声明状态 POJO ====================
    @Setter
    @Getter
    public static class State {
        private List<ChatMessage> messages = new ArrayList<>();
    }

    // ==================== 2. 定义工具类 (使用 @AiFunction 注解) ====================
    /**
     * 工具类 —— 用 @AiFunction 注解标记的方法会自动被 CompiledGraph.registerTools() 解析，
     * 生成 GptTool 元数据发给大模型，并在大模型返回 tool_calls 时自动反射调用。
     */
    public static class MyWeatherTools {

        @AiFunction(name = "get_current_weather", description = "获取指定城市的当前天气情况")
        public static String getCurrentWeather(
                @AiFunctionParam(name = "location", description = "城市名称，例如：北京、上海") String location,
                @AiFunctionParam(name = "unit", description = "温度单位，celsius(摄氏度)或fahrenheit(华氏度)", required = false) String unit
        ) {
            System.out.println(String.format("  🌤️ [get_current_weather] location=%s, unit=%s", location, unit));
            if (location.contains("北京")) {
                return "北京今天是晴天，气温 25 摄氏度。";
            } else if (location.contains("上海")) {
                return "上海今天是阴天，有小雨，气温 22 摄氏度。";
            } else {
                return location + " 的天气是多云，气温 20 摄氏度。";
            }
        }

        @AiFunction(name = "calculator", description = "执行基本的数学运算：加法、减法、乘法、除法")
        public static int calculator(
                @AiFunctionParam(name = "a", description = "第一个数字") int a,
                @AiFunctionParam(name = "b", description = "第二个数字") int b,
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
    }

    // ==================== 3. 初始化：构建拓扑 & 编译图 ====================
    @BeforeAll
    public static void init() {
        // 3.1 从 fast-openai.properties 自动读取配置 (api key, proxy, timeout 等)
        OpenAiConfig config = OpenAiConfig.loadFromProperties("fast-openai.properties");
        System.out.println("✅ 加载配置: baseUrl=" + config.getBaseUrl() +
                ", keys=" + (config.getApiKeys() != null ? config.getApiKeys().size() : 0) +
                ", proxy=" + config.getProxy() +
                ", timeout=" + config.getTimeout());
        service = new OpenAiService(config);

        // 3.2 构建 StateGraph 并注册 Reducer（messages 追加模式）
        StateGraph<State> graph = new StateGraph<>(State::new);
        graph.registerReducer("messages", (oldVal, newVal) -> {
            List<ChatMessage> merged = new ArrayList<>();
            if (oldVal != null) merged.addAll((Collection<ChatMessage>) oldVal);
            if (newVal != null) merged.addAll((Collection<ChatMessage>) newVal);
            return merged;
        });

        // 3.3 先编译图，再注册工具（因为 OpenAiAgentNode 需要 compiled 引用）
        compiled = graph.compile(new InMemorySaver())
                .registerTools(new MyWeatherTools());

        System.out.println("✅ 已注册工具: " + compiled.getGptTools().stream()
                .map(t -> t.getFunction().getName())
                .toArray());

        // 🌟 3.4 [Node] agent 节点：一句话调用通用大模型决策节点
        graph.addNode("agent", state -> OpenAiAgentNode.create(service, "gpt-5.4", compiled).apply(state));

        // 🌟 3.5 [Node] action 节点：一句话调用通用高并发工具并行节点
        graph.addNode("action", state -> OpenAiActionNode.create(compiled).apply(state));

        // 3.6 [Edges] 编排连线
        graph.addEdge(StateGraph.START, "agent");
        graph.addConditionalEdges("agent",
                state -> {
                    ChatMessage last = state.getMessages().get(state.getMessages().size() - 1);
                    return (last.getToolCalls() != null && !last.getToolCalls().isEmpty()) ? "action" : "end";
                },
                new HashMap<String, String>() {{
                    put("action", "action");
                    put("end", StateGraph.END);
                }});
        graph.addEdge("action", "agent");

        System.out.println("✅ Graph 编译完成，ReAct Agent 就绪！");
    }

    // ==================== 4. 测试用例 ====================

    /**
     * 测试 ReAct Agent：同时查询北京和上海天气，并求和两地气温
     */
    @Test
    public void testCleanReActAgent() {
        State state = new State();
        state.getMessages().add(ChatMessage.user("你好，请帮我同时查询北京和上海的天气。拿到两地气温数字后，请帮我把它们相加求和！"));

        // 一键执行
        State finalState = compiled.invoke(state, GraphConfig.builder().threadId("react-clean-v4").build());

        System.out.println("\n--- [ReAct Stream Finished] ---");
        for (ChatMessage m : finalState.getMessages()) {
            System.out.println(String.format("  [%s]: %s", m.getRole(), m.getContent()));
        }

        ChatMessage lastMsg = finalState.getMessages().get(finalState.getMessages().size() - 1);
        assertEquals("assistant", lastMsg.getRole());
        // 验证最终结果包含求和结果：25 + 22 = 47
        assertTrue(lastMsg.getContent().contains("47"), 
                "期望最终回复包含求和结果 47，实际: " + lastMsg.getContent());
    }

    /**
     * 测试 ReAct Agent：图片分析（多模态视觉理解）
     */
    @Test
    public void testImageReActAgent() {
        State state = new State();
        // 使用本地图片
        File imageFile = new File("/Users/zengshilin/work/fast-openai-sdk/src/test/resources/img.png");
        if (!imageFile.exists()) {
            System.out.println("⚠️ 图片文件不存在: " + imageFile.getAbsolutePath() + "，跳过测试。");
            return;
        }
        // 🌟 使用 ChatMessage.image() 一键创建携带图片的消息
        state.getMessages().add(ChatMessage.image("图片里面有什么？", imageFile));

        // 一键执行
        State finalState = compiled.invoke(state, GraphConfig.builder()
                .threadId("react-image-v4")
                .build());

        System.out.println("\n--- [Image ReAct Stream Finished] ---");
        for (ChatMessage m : finalState.getMessages()) {
            System.out.println(String.format("  [%s]: %s", m.getRole(), m.getContent()));
        }

        // 验证最后一条消息是 assistant 角色
        ChatMessage lastMsg = finalState.getMessages().get(finalState.getMessages().size() - 1);
        assertEquals("assistant", lastMsg.getRole());
        assertNotNull(lastMsg.getContent(), "期望 assistant 有回复内容");
    }
}
