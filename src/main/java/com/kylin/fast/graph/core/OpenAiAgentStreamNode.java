package com.kylin.fast.graph.core;

import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.graph.model.ToolCall;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.*;
import com.kylin.fast.openai.result.ChatStreamResult;
import com.kylin.fast.openai.result.dto.ChatCompletionStreamChoice;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 🌟 OpenAI 流式 Agent 决策节点 — 真 chunk 级别 token 透传版
 * <p>
 * 与 {@link OpenAiAgentNode} 功能对等，使用 {@link OpenAiService#createChatStream}
 * 实现逐 chunk 实时 token 发射到 {@link GraphContext#getEmitter()}。
 * <p>
 * 🔧 无需手动传入 {@link CompiledGraph} —— 执行时自动从 {@link GraphContext} 获取。
 * 🔧 支持链式配置：{@code .parallelToolCalls(true).reasoningEffort("medium").logEnabled(true)} 等。
 * <p>
 * 使用方式：
 * <pre>
 * graph.addNode("agent", state ->
 *     OpenAiAgentStreamNode.create(service, "gpt-5.4")
 *         .parallelToolCalls(true)
 *         .reasoningEffort("high")
 *         .logEnabled(true)
 *         .apply(state));
 *
 * // 可选：传入 GraphStopwatch 进行全链路耗时监控
 * GraphStopwatch sw = GraphStopwatch.create("MyGraph");
 * graph.addNode("agent", state ->
 *     OpenAiAgentStreamNode.create(service, "gpt-5.4")
 *         .stopwatch(sw)
 *         .apply(state));
 * </pre>
 */
@Slf4j
public class OpenAiAgentStreamNode<S> implements Node<S> {

    private final OpenAiService service;
    private final String model;

    // ────────── 可选的 ChatRequest 覆盖字段 ──────────
    private Boolean parallelToolCalls;
    private String reasoningEffort;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private String toolChoice;
    private boolean logEnabled;
    private GraphStopwatch stopwatch;

    public OpenAiAgentStreamNode(OpenAiService service, String model) {
        this.service = service;
        this.model = model;
    }

    public static <S> OpenAiAgentStreamNode<S> create(OpenAiService service, String model) {
        return new OpenAiAgentStreamNode<>(service, model);
    }

    // ────────── 链式配置 ──────────

    public OpenAiAgentStreamNode<S> parallelToolCalls(Boolean v) { this.parallelToolCalls = v; return this; }
    public OpenAiAgentStreamNode<S> reasoningEffort(String v) { this.reasoningEffort = v; return this; }
    public OpenAiAgentStreamNode<S> temperature(Double v) { this.temperature = v; return this; }
    public OpenAiAgentStreamNode<S> maxTokens(Integer v) { this.maxTokens = v; return this; }
    public OpenAiAgentStreamNode<S> topP(Double v) { this.topP = v; return this; }
    public OpenAiAgentStreamNode<S> toolChoice(String v) { this.toolChoice = v; return this; }
    public OpenAiAgentStreamNode<S> logEnabled(boolean v) { this.logEnabled = v; return this; }

    /**
     * 链式配置：传入 GraphStopwatch 进行耗时监控（可选）
     */
    public OpenAiAgentStreamNode<S> stopwatch(GraphStopwatch sw) {
        this.stopwatch = sw;
        return this;
    }

    @Override
    public Map<String, Object> apply(S state) {
        final boolean timing = this.stopwatch != null;
        if (timing) this.stopwatch.start("agent");

        try {
            // 1. 反射获取 messages
            final long t1 = timing ? System.currentTimeMillis() : 0;

            java.lang.reflect.Method getMessagesMethod = state.getClass().getMethod("getMessages");
            @SuppressWarnings("unchecked")
            List<ChatMessage> chatMessages = (List<ChatMessage>) getMessagesMethod.invoke(state);

            // 2. ChatMessage → SDK BaseMessage
            List<BaseMessage> sdkMessages = convertMessages(chatMessages);

            // 3. 构造 ChatRequest
            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(sdkMessages)
                    .build();

            // 4. 🌟 从 GraphContext 自动获取 compiled，注入工具声明
            CompiledGraph<S> compiled = GraphContext.getCompiledGraph();
            if (compiled != null && compiled.getGptTools() != null) {
                request.setTools(compiled.getGptTools());
                request.setParallelToolCalls(this.parallelToolCalls != null ? this.parallelToolCalls : true);
            }

            // 5. 🌟 应用用户配置字段
            if (this.reasoningEffort != null)  request.setReasoningEffort(this.reasoningEffort);
            if (this.temperature != null)      request.setTemperature(this.temperature);
            if (this.maxTokens != null)        request.setMaxTokens(this.maxTokens);
            if (this.topP != null)             request.setTopP(this.topP);
            if (this.toolChoice != null)       request.setToolChoice(this.toolChoice);

            if (timing) {
                this.stopwatch.lap("build-request", System.currentTimeMillis() - t1);
            }

            // 6. 🔍 请求前日志
            if (logEnabled) {
                log.info("gpt stream request model:{} messages:{} tools:{}", model, sdkMessages.size(),
                        compiled != null && compiled.getGptTools() != null ? compiled.getGptTools().size() : 0);
            }

            // 7. 🌟 流式调用：逐 chunk 发射 token + 累积 tool calls
            final long t2 = timing ? System.currentTimeMillis() : 0;

            StreamEmitter emitter = GraphContext.getEmitter();
            StringBuilder textBuilder = new StringBuilder();
            Map<Integer, ToolCallAccumulator> toolCallMap = new LinkedHashMap<>();
            AtomicReference<Exception> streamError = new AtomicReference<>();
            AtomicReference<String> finishReason = new AtomicReference<>();

            service.createChatStream(request, (ChatStreamResult result, boolean isDone) -> {
                try {
                    if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                        ChatCompletionStreamChoice choice = result.getChoices().get(0);
                        if (choice.getDelta() != null) {
                            // 发射 text token
                            String content = choice.getDelta().getContent();
                            if (content != null && !content.isEmpty()) {
                                textBuilder.append(content);
                                if (emitter != null) {
                                    emitter.emit(new GraphEvent("token", "agent", content));
                                }
                            }
                            // 累积 function call 片段
                            if (choice.getDelta().getToolCalls() != null) {
                                for (int i = 0; i < choice.getDelta().getToolCalls().size(); i++) {
                                    GptTool tc = choice.getDelta().getToolCalls().get(i);
                                    int idx = tc.getIndex() != null ? tc.getIndex() : i;
                                    ToolCallAccumulator acc = toolCallMap.computeIfAbsent(idx, k -> new ToolCallAccumulator());
                                    if (tc.getId() != null) acc.id = tc.getId();
                                    if (tc.getFunction() != null) {
                                        if (tc.getFunction().getName() != null) acc.name = tc.getFunction().getName();
                                        if (tc.getFunction().getArguments() != null)
                                            acc.args.append(tc.getFunction().getArguments());
                                    }
                                }
                            }
                        }
                        // 记录 finish_reason
                        if (choice.getFinish_reason() != null) {
                            finishReason.set(choice.getFinish_reason());
                        }
                    }
                } catch (Exception e) {
                    streamError.set(e);
                }
            });

            if (timing) {
                this.stopwatch.lap("api-call", System.currentTimeMillis() - t2);
            }

            if (streamError.get() != null) {
                throw new RuntimeException("Stream processing failed", streamError.get());
            }

            // 8. 构建 ToolCall 列表
            final long t3 = timing ? System.currentTimeMillis() : 0;

            List<ToolCall> toolCalls = new ArrayList<>();
            for (ToolCallAccumulator acc : toolCallMap.values()) {
                if (acc.name != null && !acc.name.isEmpty()) {
                    toolCalls.add(new ToolCall(
                            acc.id != null ? acc.id : UUID.randomUUID().toString(),
                            acc.name,
                            acc.args.toString()
                    ));
                }
            }

            // 9. 🔍 请求后日志
            if (logEnabled) {
                log.info("gpt stream response model:{} content:{} toolCalls:{} finishReason:{}",
                        model,
                        textBuilder,
                        toolCalls.isEmpty() ? "none" : toolCalls.stream().map(ToolCall::getName).collect(Collectors.toList()),
                        finishReason.get());
            }

            // 10. 回填消息
            Map<String, Object> update = new HashMap<>();
            if (!toolCalls.isEmpty()) {
                String finalContent = textBuilder.length() > 0 ? textBuilder.toString() : null;
                update.put("messages", Collections.singletonList(ChatMessage.assistant(finalContent, toolCalls)));
            } else {
                update.put("messages", Collections.singletonList(ChatMessage.assistant(textBuilder.toString())));
            }

            if (timing) {
                this.stopwatch.lap("build-response", System.currentTimeMillis() - t3);
            }

            return update;

        } catch (Exception e) {
            throw new RuntimeException("OpenAiAgentStreamNode execution failed: " + e.getMessage(), e);
        } finally {
            if (timing) {
                this.stopwatch.end("agent");
            }
        }
    }

    private List<BaseMessage> convertMessages(List<ChatMessage> chatMessages) {
        List<BaseMessage> sdkMessages = new ArrayList<>();
        for (ChatMessage m : chatMessages) {
            if ("image".equals(m.getMessageType()) && m.getImageUrls() != null && !m.getImageUrls().isEmpty()) {
                List<ImgMessageContent> contents = new ArrayList<>();
                if (StringUtils.isNotBlank(m.getContent())) {
                    contents.add(ImgMessageContent.builder().type("text").text(m.getContent()).build());
                }
                for (String url : m.getImageUrls()) {
                    contents.add(ImgMessageContent.builder()
                            .type("image_url")
                            .image_url(ImageUrl.builder().url(url).detail("high").build())
                            .build());
                }
                sdkMessages.add(ImgMessage.builder().role(m.getRole()).content(contents).build());
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
        return sdkMessages;
    }

    private static class ToolCallAccumulator {
        String id;
        String name;
        StringBuilder args = new StringBuilder();
    }
}
