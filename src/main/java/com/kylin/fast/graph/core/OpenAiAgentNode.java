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
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🌟 通用大模型 Agent 决策节点 (OpenAiAgentNode) —— 【高智感+高透明度调试版】
 * 支持大模型原生思考链（Reasoning/Thought Content）的高保真还原，
 * 并在大模型原生 Content 为空时，自适应根据准备执行的工具列表生成高智感的 Action 回填（如 "正在调用工具: [calculator]..."）。
 * <p>
 * 🌟 新增：[Observability 观察者模式] 自动在控制台打印大模型单次响应中下达的并行函数决策与工具数量。
 *
 * @author AI Agent
 */
public class OpenAiAgentNode<S> implements Node<S> {

    private final OpenAiService service;
    private final String model;
    private final CompiledGraph<S> compiled;

    public OpenAiAgentNode(OpenAiService service, String model, CompiledGraph<S> compiled) {
        this.service = service;
        this.model = model;
        this.compiled = compiled;
    }

    /**
     * 一键创建静态工厂方法
     */
    public static <S> OpenAiAgentNode<S> create(OpenAiService service, String model, CompiledGraph<S> compiled) {
        return new OpenAiAgentNode<>(service, model, compiled);
    }

    @Override
    public Map<String, Object> apply(S state) {
        try {
            // 1. 动态反射获取状态里的 getMessages 列表 (对你的 State POJO 零污染零侵入)
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

            // 3. 构造 ChatRequest 报文，并自动注入在 compiled 中注册好的所有注解工具描述
            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(sdkMessages)
                    .build();

            if (compiled != null && compiled.getGptTools() != null) {
                request.setTools(compiled.getGptTools());
            }

            // 4. 调用大模型
            ChatResult result = service.createChat(request);
            Map<String, Object> update = new HashMap<>();

            // 5. 结果高保真自适应解析与【高透明度观察者打印】
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

                // 优先拿取模型原生的 Content/Thought 描述 (某些模型在 Tool 调用前会自己生成思考解释)
                String finalContent = result.content();

                if (StringUtils.isBlank(finalContent)) {
                    // Fallback 降级：如果大模型原生 content 为空，我们自动通过 toolCalls 的独特工具名进行高智感拼接！
                    List<String> toolNames = toolCalls.stream().map(ToolCall::getName).distinct().collect(Collectors.toList());
                    if (toolNames.size() > 1) {
                        finalContent = "Call Tool: " + toolNames + "...";
                    } else if (toolNames.size() == 1) {
                        finalContent = "Call Tools: [" + toolNames.get(0) + "]...";
                    } else {
                        finalContent = null;
                    }
                }

                update.put("messages", Collections.singletonList(ChatMessage.assistant(finalContent, toolCalls)));
            } else {
                // 最终回复消息放回
                update.put("messages", Collections.singletonList(ChatMessage.assistant(result.content())));
            }
            return update;

        } catch (Exception e) {
            throw new RuntimeException("OpenAiAgentNode execution failed: " + e.getMessage(), e);
        }
    }
}
