package com.kylin.fast.graph.core;

import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.graph.model.ToolCall;
import com.kylin.fast.openai.request.dto.Message;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🌟 通用工具并行反射执行节点 (OpenAiActionNode)
 * 一句话极简添加工具节点：自动从状态中提取大模型最新下达的 ToolCalls 指令，
 * 引入 [.parallelStream()] 管道，完美高并发、多线程并行调度在 CompiledGraph 里注册的所有注解工具方法！
 * 自动拦截错误、汇总生成 tool 报文并回传状态。
 * <p>
 * 🔧 无需手动传入 {@link CompiledGraph} —— 执行时自动从 {@link GraphContext} 获取。
 * <p>
 * 使用方式：
 * <pre>
 * graph.addNode("action", state -> OpenAiActionNode.create().apply(state));
 *
 * // 可选：传入 GraphStopwatch 进行全链路耗时监控
 * GraphStopwatch sw = GraphStopwatch.create("MyGraph");
 * graph.addNode("action", state -> OpenAiActionNode.create().stopwatch(sw).apply(state));
 * </pre>
 *
 * @author AI Agent
 */
public class OpenAiActionNode<S> implements Node<S> {

    private GraphStopwatch stopwatch;

    public OpenAiActionNode() {}

    /**
     * 一键创建静态工厂方法
     */
    public static <S> OpenAiActionNode<S> create() {
        return new OpenAiActionNode<>();
    }

    /**
     * 链式配置：传入 GraphStopwatch 进行耗时监控（可选）
     */
    public OpenAiActionNode<S> stopwatch(GraphStopwatch sw) {
        this.stopwatch = sw;
        return this;
    }

    @Override
    public Map<String, Object> apply(S state) {
        final boolean timing = this.stopwatch != null;
        final long t0 = timing ? System.currentTimeMillis() : 0;
        if (timing) this.stopwatch.start("action");

        try {
            // 1. 动态反射获取 getMessages 列表 (对 State POJO 零污染零侵入)
            Method getMessagesMethod = state.getClass().getMethod("getMessages");
            @SuppressWarnings("unchecked")
            List<ChatMessage> chatMessages = (List<ChatMessage>) getMessagesMethod.invoke(state);

            if (chatMessages == null || chatMessages.isEmpty()) {
                return Collections.emptyMap();
            }

            // 2. 提取最后一条消息里的所有 ToolCall 列表
            ChatMessage lastAgentMsg = chatMessages.get(chatMessages.size() - 1);
            List<ToolCall> tcs = lastAgentMsg.getToolCalls();

            List<ChatMessage> toolResults = new ArrayList<>();
            if (tcs != null && !tcs.isEmpty()) {
                // 🌟 从 GraphContext 获取 compiled
                CompiledGraph<S> compiled = GraphContext.getCompiledGraph();
                if (compiled == null) {
                    throw new IllegalStateException("CompiledGraph not found in GraphContext. "
                            + "OpenAiActionNode must run inside a CompiledGraph execution.");
                }

                System.out.println(String.format("\n[OpenAiActionNode] 💡 图引擎检测到 %d 个待执行工具，正在发起【多线程高并发】并行调度反射...", tcs.size()));

                // 🌟 3. 引入 Java 8 parallelStream()：多任务自动并发分发、多核性能全力释放，零阻塞！
                final CompiledGraph<S> finalCompiled = compiled;
                final GraphStopwatch finalSw = this.stopwatch;
                toolResults = tcs.parallelStream()
                        .map(tc -> {
                            System.out.println(String.format("  -> [线程: %s] 正在高并发调用反射注解方法: %s",
                                    Thread.currentThread().getName(), tc.getName()));

                            final long toolT0 = timing ? System.currentTimeMillis() : 0;
                            Message sdkToolMsg = finalCompiled.executeTool(tc.getName(), tc.getArguments(), tc.getId());
                            if (timing) {
                                finalSw.lap("tool:" + tc.getName(), System.currentTimeMillis() - toolT0);
                            }
                            if (sdkToolMsg != null) {
                                return ChatMessage.tool(tc.getName(), sdkToolMsg.getContent(), tc.getId());
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                System.out.println("[OpenAiActionNode] 💡 所有并行注解工具反射执行完毕，已汇总结果。");
            }

            Map<String, Object> update = new HashMap<>();
            update.put("messages", toolResults);
            return update;

        } catch (Exception e) {
            throw new RuntimeException("OpenAiActionNode execution failed: " + e.getMessage(), e);
        } finally {
            if (timing) {
                this.stopwatch.end("action");
            }
        }
    }
}
