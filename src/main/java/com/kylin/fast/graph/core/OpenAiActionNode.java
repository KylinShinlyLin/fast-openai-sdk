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
 *
 * @author AI Agent
 */
public class OpenAiActionNode<S> implements Node<S> {

    private final CompiledGraph<S> compiled;

    public OpenAiActionNode(CompiledGraph<S> compiled) {
        this.compiled = compiled;
    }

    /**
     * 一键创建静态工厂方法
     */
    public static <S> OpenAiActionNode<S> create(CompiledGraph<S> compiled) {
        return new OpenAiActionNode<>(compiled);
    }

    @Override
    public Map<String, Object> apply(S state) {
        try {
            // 1. 动态反射获取 getMessages 列表 (对你的 State POJO 零污染零侵入)
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
                System.out.println(String.format("\n[OpenAiActionNode] 💡 图引擎检测到 %d 个待执行工具，正在发起【多线程高并发】并行调度反射...", tcs.size()));

                // 🌟 3. 引入 Java 8 parallelStream()：多任务自动并发分发、多核性能全力释放，零阻塞！
                toolResults = tcs.parallelStream()
                        .map(tc -> {
                            System.out.println(String.format("  -> [线程: %s] 正在高并发调用反射注解方法: %s", 
                                    Thread.currentThread().getName(), tc.getName()));
                            
                            Message sdkToolMsg = compiled.executeTool(tc.getName(), tc.getArguments(), tc.getId());
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
        }
    }
}
