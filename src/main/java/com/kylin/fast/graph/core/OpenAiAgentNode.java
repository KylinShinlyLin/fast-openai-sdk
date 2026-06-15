package com.kylin.fast.graph.core;

import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.graph.model.ToolCall;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.BaseMessage;
import com.kylin.fast.openai.request.dto.GptFunction;
import com.kylin.fast.openai.request.dto.GptTool;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.request.dto.ImgMessage;
import com.kylin.fast.openai.request.dto.ImgMessageContent;
import com.kylin.fast.openai.request.dto.ImageUrl;
import com.kylin.fast.openai.result.ChatResult;
import com.kylin.fast.openai.result.dto.ChatChoice;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 🌟 通用大模型 Agent 决策节点 (OpenAiAgentNode) —— 【高智感+高透明度调试版】
 * 支持大模型原生思考链（Reasoning/Thought Content）的高保真还原，
 * 并在大模型原生 Content 为空时，自适应根据准备执行的工具列表生成高智感的 Action 回填。
 * <p>
 * 🔧 无需手动传入 {@link CompiledGraph} —— 执行时自动从 {@link GraphContext} 获取。
 * 🔧 支持链式配置：{@code .parallelToolCalls(true).reasoningEffort("medium").logEnabled(true)} 等。
 * <p>
 * 使用方式：
 * <pre>
 * graph.addNode("agent", state ->
 *     OpenAiAgentNode.create(service, "Kimi-K2.6")
 *         .parallelToolCalls(true)
 *         .reasoningEffort("medium")
 *         .logEnabled(true)
 *         .apply(state));
 *
 * // 可选：传入 GraphStopwatch 进行全链路耗时监控
 * GraphStopwatch sw = GraphStopwatch.create("MyGraph");
 * graph.addNode("agent", state ->
 *     OpenAiAgentNode.create(service, "gpt-4")
 *         .stopwatch(sw)
 *         .apply(state));
 * </pre>
 *
 * @author AI Agent
 */
@Slf4j
public class OpenAiAgentNode<S> implements Node<S> {

    private final OpenAiService service;
    private final String model;
    private String promptCacheKey;
    private String promptCacheRetention;

    // ────────── 可选的 ChatRequest 覆盖字段 ──────────
    private Boolean parallelToolCalls;
    private String reasoningEffort;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private String toolChoice;
    private boolean logEnabled;
    private GraphStopwatch stopwatch;

    public OpenAiAgentNode(OpenAiService service, String model) {
        this.service = service;
        this.model = model;
    }

    /**
     * 一键创建静态工厂方法
     */
    public static <S> OpenAiAgentNode<S> create(OpenAiService service, String model) {
        return new OpenAiAgentNode<>(service, model);
    }

    // ────────── 链式配置 ──────────

    /**
     * 是否开启并行工具调用（默认：有工具时自动开启）
     */
    public OpenAiAgentNode<S> parallelToolCalls(Boolean v) {
        this.parallelToolCalls = v;
        return this;
    }

    /**
     * 推理深度：low / medium / high
     */
    public OpenAiAgentNode<S> reasoningEffort(String v) {
        this.reasoningEffort = v;
        return this;
    }

    /**
     * 采样温度 (0~2)
     */
    public OpenAiAgentNode<S> temperature(Double v) {
        this.temperature = v;
        return this;
    }

    /**
     * 最大输出 token 数
     */
    public OpenAiAgentNode<S> maxTokens(Integer v) {
        this.maxTokens = v;
        return this;
    }

    /**
     * Top-P 核采样
     */
    public OpenAiAgentNode<S> topP(Double v) {
        this.topP = v;
        return this;
    }

    /**
     * 工具选择策略：auto / none / required / {"type":"function","function":{"name":"xxx"}}
     */
    public OpenAiAgentNode<S> toolChoice(String v) {
        this.toolChoice = v;
        return this;
    }

    /**
     * Prompt 缓存 key（同一个 key 复用缓存），不设则自动生成随机 UUID
     */
    public OpenAiAgentNode<S> promptCacheKey(String v) { this.promptCacheKey = v; return this; }

    /**
     * Prompt 缓存保留时长，如 "24h" / "1h" / "30m"
     */
    public OpenAiAgentNode<S> promptCacheRetention(String v) { this.promptCacheRetention = v; return this; }

    /**
     * 是否通过日志打印请求和响应
     */
    public OpenAiAgentNode<S> logEnabled(boolean v) {
        this.logEnabled = v;
        return this;
    }

    /**
     * 链式配置：传入 GraphStopwatch 进行耗时监控（可选）
     */
    public OpenAiAgentNode<S> stopwatch(GraphStopwatch sw) {
        this.stopwatch = sw;
        return this;
    }

    @Override
    public Map<String, Object> apply(S state) {
        final boolean timing = this.stopwatch != null;
        if (timing) this.stopwatch.start("agent");

        try {
            // 1. 动态反射获取状态里的 getMessages 列表 (对 State POJO 零污染零侵入)
            final long t1 = timing ? System.currentTimeMillis() : 0;

            Method getMessagesMethod = state.getClass().getMethod("getMessages");
            @SuppressWarnings("unchecked")
            List<ChatMessage> chatMessages = (List<ChatMessage>) getMessagesMethod.invoke(state);

            // 2. ChatMessage 转换为 SDK BaseMessage
            List<BaseMessage> sdkMessages = new ArrayList<>();
            for (ChatMessage m : chatMessages) {
                if ("image".equals(m.getMessageType()) && m.getImageUrls() != null && !m.getImageUrls().isEmpty()) {
                    List<ImgMessageContent> contents = new ArrayList<>();
                    if (StringUtils.isNotBlank(m.getContent())) {
                        contents.add(ImgMessageContent.builder()
                                .type("text")
                                .text(m.getContent())
                                .build());
                    }
                    for (String url : m.getImageUrls()) {
                        contents.add(ImgMessageContent.builder()
                                .type("image_url")
                                .image_url(ImageUrl.builder().url(url).detail("high").build())
                                .build());
                    }
                    sdkMessages.add(ImgMessage.builder()
                            .role(m.getRole())
                            .content(contents)
                            .build());
                } else {
                    Message sdkMsg = new Message();
                    sdkMsg.setRole(m.getRole());
                    sdkMsg.setContent(m.getContent());
                    sdkMsg.setName(m.getName());
                    sdkMsg.setToolCallId(m.getToolCallId());
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
            }

            // 3. 构造 ChatRequest 报文
            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(sdkMessages)
                    .build();

            // 4. 🌟 从 GraphContext 自动获取 compiled，注入工具声明
            CompiledGraph<S> compiled = GraphContext.getCompiledGraph();
            if (compiled != null && compiled.getGptTools() != null && !compiled.getGptTools().isEmpty()) {
                request.setTools(compiled.getGptTools());
                // 默认：有工具时开启并行调用（用户可通过 .parallelToolCalls(false) 覆盖）
                request.setParallelToolCalls(this.parallelToolCalls != null ? this.parallelToolCalls : true);
                if (this.model != null && (this.model.startsWith("gpt-5.4") || this.model.startsWith("gpt-5.5"))) {
                    if (this.promptCacheKey == null) {
                        this.promptCacheKey = UUID.randomUUID().toString();
                    }
                    request.setPromptCacheKey(this.promptCacheKey);
                    request.setPromptCacheRetention(this.promptCacheRetention != null ? this.promptCacheRetention : "24h");
                }
            }

            // 5. 🌟 应用用户通过链式配置设置的字段
            if (this.reasoningEffort != null) request.setReasoningEffort(this.reasoningEffort);
            if (this.temperature != null) request.setTemperature(this.temperature);
            if (this.maxTokens != null) request.setMaxTokens(this.maxTokens);
            if (this.topP != null) request.setTopP(this.topP);
            if (this.toolChoice != null) request.setToolChoice(this.toolChoice);

            if (timing) {
                this.stopwatch.lap("build-request", System.currentTimeMillis() - t1);
            }

            // 6. 🔍 请求前日志
            if (logEnabled) {
                log.info("gpt request model:{} messages:{} tools:{}", model, sdkMessages.size(),
                        compiled != null && compiled.getGptTools() != null ? compiled.getGptTools().size() : 0);
            }

            // 7. 调用大模型
            final long t2 = timing ? System.currentTimeMillis() : 0;
            ChatResult result = service.createChat(request);
            if (timing) {
                this.stopwatch.lap("api-call", System.currentTimeMillis() - t2);
            }

            // 8. 🔍 请求后日志
            if (logEnabled) {
                log.info("gpt response model:{} content:{} toolCalls:{} finishReason:{} usage:{}",
                        result.getModel(),
                        result.content(),
                        result.triggerFunction() ? result.getChoices().get(0).getMessage().getToolCalls() : "none",
                        result.getChoices() != null && !result.getChoices().isEmpty() ? result.getChoices().get(0).getFinish_reason() : "unknown",
                        result.getUsage());
            }

            final long t3 = timing ? System.currentTimeMillis() : 0;

            Map<String, Object> update = new HashMap<>();

            // 9. 结果高保真自适应解析与【高透明度观察者打印】
            if (result.triggerFunction()) {
                // 触发工具调用：转换为 ToolCall Assistant 消息放回
                List<ToolCall> toolCalls = new ArrayList<>();
                for (ChatChoice choice : result.getChoices()) {
                    if (choice.getMessage() != null && choice.getMessage().getToolCalls() != null) {
                        for (GptTool gt : choice.getMessage().getToolCalls()) {
                            if (gt.getFunction() != null) {
                                toolCalls.add(new ToolCall(
                                        gt.getId(),
                                        gt.getFunction().getName(),
                                        gt.getFunction().getArguments()
                                ));
                            }
                        }
                    }
                }

                // 优先拿取模型原生的 Content/Thought 描述
                // 注意：当 LLM 返回 tool_calls 时 content 可以为 null（OpenAI 协议允许），
                // 不要生成 "Call Tools: [...]" 占位文本，避免下一轮 LLM 被误导而重复调用同一工具。
                String finalContent = result.content();

                update.put("messages", Collections.singletonList(ChatMessage.assistant(finalContent, toolCalls)));
            } else {
                // 最终回复消息放回
                update.put("messages", Collections.singletonList(ChatMessage.assistant(result.content())));
            }

            if (timing) {
                this.stopwatch.lap("build-response", System.currentTimeMillis() - t3);
            }

            return update;

        } catch (Exception e) {
            throw new RuntimeException("OpenAiAgentNode execution failed: " + e.getMessage(), e);
        } finally {
            if (timing) {
                this.stopwatch.end("agent");
            }
        }
    }
}
