# Fast OpenAI SDK

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://openjdk.java.net/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kylinshinlylin/fast-openai-sdk.svg)](https://central.sonatype.com/artifact/io.github.kylinshinlylin/fast-openai-sdk)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

**Fast OpenAI SDK** 是一个专为 Java 开发者设计的轻量级、高性能 SDK，旨在提供与 OpenAI 及 Jina API 完美兼容的客户端调用接口。它不仅具备极简的接入体验，还内置了流式响应（SSE）、多 API Key 负载均衡、请求重试、视觉能力、大模型函数调用（Function Calling）以及全新的 **Graph Agent 智能体编排引擎**等核心企业级特性。

## ✨ 核心特性

- **🚀 极致轻量与兼容**：**纯 Java 8 开发**，零 Spring Boot 等重量级依赖，无侵入性。可无缝集成到各类 Java 应用程序。
- **⚡ 高性能网络层**：底层基于业界成熟的 **Retrofit2 & OkHttp3** 构建，确保网络请求的高吞吐与低延时。
- **💬 完整兼容大模型 API**：全面支持文本生成（Chat）、打字机流式输出（SSE）、多模态视觉（Vision）、工具调用（Function Calling）等核心能力。
- **🤖 Graph Agent 智能体引擎**：内置 StateGraph 有向状态图引擎，支持 ReAct 推理-行动循环、条件路由、并行工具调用、流式 Token 透传、Checkpoint 持久化与中断恢复，几行代码即可构建复杂的 AI Agent 工作流。
- **⚖️ 智能负载均衡**：原生支持配置多个 API Key 请求时自动轮询，有效突破单一 Token 限流瓶颈并分摊计费压力。
- **🔌 灵活的扩展配置**：可通过 `fast-openai.properties` 或 Java API 动态配置 HTTP/SOCKS 代理、超时时间、重试策略等。
- **🔍 Jina 生态扩展**：内置对 Jina 搜索、Reader 相关生态接口的适配支持。

---

## 📖 目录索引

- [📦 快速安装](#-快速安装)
- [🚀 快速开始](#-快速开始)
  - [1. 初始化配置](#1-初始化配置)
  - [2. 基础对话 (同步请求)](#2-基础对话-同步请求)
  - [3. 流式对话 (SSE 打字机效果)](#3-流式对话-sse-打字机效果)
  - [4. 视觉能力 (多模态图片理解)](#4-视觉能力-多模态图片理解)
  - [5. 函数调用 (Function Calling)](#5-函数调用-function-calling)
  - [6. 文本转语音 (TTS)](#6-文本转语音-tts)
  - [7. 语音转文本 (Whisper ASR)](#7-语音转文本-whisper-asr)
  - [8. Graph Agent 智能体编排](#8-graph-agent-智能体编排)
    - [8.1 StateGraph 基础用法](#81-stategraph-基础用法)
    - [8.2 ReAct Agent 实战](#82-react-agent-实战)
    - [8.3 流式执行与 Token 透传](#83-流式执行与-token-透传)
    - [8.4 Checkpoint 持久化与中断恢复](#84-checkpoint-持久化与中断恢复)
- [🗂️ 核心包结构说明](#️-核心包结构与架构说明)
- [🤝 参与贡献](#-参与贡献)

---

## 📦 快速安装

在您的项目中引入依赖：

**Maven** (`pom.xml`)
```xml
<dependency>
    <groupId>io.github.kylinshinlylin</groupId>
    <artifactId>fast-openai-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle** (`build.gradle`)
```groovy
implementation 'io.github.kylinshinlylin:fast-openai-sdk:1.0.0'
```

---

## 🚀 快速开始

### 1. 初始化配置

**方式一：配置文件（推荐）**

在您的 `src/main/resources` 目录下创建 `fast-openai.properties` 配置文件，SDK 会自动识别并加载：

```properties
# OpenAI API Keys，多个 Key 之间使用英文逗号分隔，SDK 会自动负载均衡
openai.api.keys=sk-your-key-1,sk-your-key-2
# OpenAI Base URL (支持自定义代理网关地址)
openai.api.baseUrl=https://api.openai.com/
# 请求超时时间（秒）
openai.api.timeout=60
# 最大重试次数
openai.api.maxRetries=3

# 可选：全局网络代理配置 (支持 HTTP / SOCKS)
# openai.proxy.host=127.0.0.1
# openai.proxy.port=7890
# openai.proxy.type=HTTP
```

**方式二：代码动态构建**

```java
import com.kylin.fast.openai.config.OpenAiConfig;
import java.util.Arrays;
import java.time.Duration;

OpenAiConfig config = new OpenAiConfig();
config.setApiKeys(Arrays.asList("sk-your-key-1", "sk-your-key-2"));
config.setBaseUrl("https://api.openai.com/");
config.setTimeout(Duration.ofSeconds(60));
config.setMaxRetries(3);
```

---

### 2. 基础对话 (同步请求)

以下示例展示如何发送一个标准的聊天补全请求并获取完整的文本结果：

```java
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.result.ChatResult;
import java.util.Collections;

public class BasicChatDemo {
    public static void main(String[] args) {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(Collections.singletonList(Message.of(MessageRole.user, "你好，请做个简短的自我介绍。")))
                .build();

        ChatResult result = service.createChat(request);
        System.out.println(result.getChoices().get(0).getMessage().getContent());
    }
}
```

---

### 3. 流式对话 (SSE 打字机效果)

当请求体参数很大或模型响应较慢时，极力推荐使用流式接口，避免主线程阻塞：

```java
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.constant.MessageRole;
import java.util.Collections;

public class StreamChatDemo {
    public static void main(String[] args) throws InterruptedException {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(Collections.singletonList(Message.of(MessageRole.user, "请为我写一首关于春天的短诗。")))
                .stream(true) // 显式声明开启流式输出
                .build();

        // 异步流式回调监听
        service.createChatStream(request, (result, isDone) -> {
            if (isDone) {
                System.out.println("\n[输出完成]");
            } else {
                if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                    String content = result.getChoices().get(0).getDelta().getContent();
                    if (content != null) {
                        System.out.print(content);
                    }
                }
            }
        });

        // 保持主线程存活以等待异步回调结束
        Thread.sleep(10000);
    }
}
```

---

### 4. 视觉能力 (多模态图片理解)

SDK 原生支持通过封装好的 `ImgMessage` 对象传递视觉参数，既支持**图片 URL 解析**，也支持**本地图片自动 Base64 编码解析**。

```java
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.ImgMessage;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.result.ChatResult;
import java.io.File;
import java.util.Collections;

public class VisionDemo {
    public static void main(String[] args) {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        // 【示例一】: 通过 URL 解析图片
        ImgMessage urlImgMessage = ImgMessage.of(MessageRole.user, "请描述一下这张图片的内容。", "https://example.com/sample.jpg");

        // 【示例二】: 通过本地文件解析图片 (SDK 内部会自动转为 Base64)
        File localImg = new File("/path/to/your/image.png");
        ImgMessage fileImgMessage = ImgMessage.of(MessageRole.user, "请描述一下这张本地图片的内容。", localImg);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o") // 或对应的 vision 模型
                .messages(Collections.singletonList(fileImgMessage)) // 这里传入对应的 ImgMessage 即可
                .build();

        ChatResult result = service.createChat(request);
        System.out.println("视觉解析结果: " + result.getChoices().get(0).getMessage().getContent());
    }
}
```

---

### 5. 函数调用 (Function Calling)

SDK 极大地简化了 Function Calling 复杂的回调处理。只需使用 `@AiFunction` 注解标记你的本地 Java 方法，并在调用请求时将你的类实例传入即可。**SDK 内部会自动解析工具、发起对话，并在大模型决定调用函数时，自动执行 Java 方法并回传结果给大模型。**

#### 步骤一：定义您的本地函数类
实现 `BaseFunctionHandler` 接口，并使用注解标记希望开放给 AI 的方法和参数：

```java
import com.kylin.fast.openai.function.handler.BaseFunctionHandler;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;

public class WeatherTool implements BaseFunctionHandler {

    @AiFunction(name = "get_weather", description = "获取指定城市的实时天气信息")
    public String getWeather(
            @AiFunctionParam(name = "city", description = "城市名称，例如：北京, 上海") String city
    ) {
        // 实际业务中，这里可发起 HTTP 请求查询真实天气系统
        return city + " 今天天气晴朗，气温 25°C。";
    }
}
```

#### 步骤二：发起带有工具的同步/异步对话

**【同步方式】:**
```java
public class FunctionCallingSyncDemo {
    public static void main(String[] args) {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(Collections.singletonList(Message.of(MessageRole.user, "北京今天的天气怎么样？出门需要带伞吗？")))
                .build();

        // 将工具实例传入 createChat，SDK 全自动完成后续轮次对话
        ChatResult result = service.createChat(request, new WeatherTool());
        System.out.println(result.getChoices().get(0).getMessage().getContent());
    }
}
```

**【流式(SSE)方式】:**
```java
public class FunctionCallingStreamDemo {
    public static void main(String[] args) throws InterruptedException {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(Collections.singletonList(Message.of(MessageRole.user, "北京今天的天气怎么样？出门需要带伞吗？")))
                .stream(true) // 开启流式输出
                .build();

        // 将工具实例一并传入，SDK 全自动拦截解析函数并在完毕后重新推送流式文本
        service.createChatStream(request, (result, isDone) -> {
            if (isDone) {
                System.out.println("\n[输出完成]");
            } else {
                if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                    String content = result.getChoices().get(0).getDelta().getContent();
                    if (content != null) {
                        System.out.print(content);
                    }
                }
            }
        }, new WeatherTool()); // <-- 传递函数处理器实例

        Thread.sleep(15000);
    }
}
```

---

### 6. 文本转语音 (TTS)

SDK 内置了对 OpenAI TTS (Text-to-Speech) 的支持，可将文本转换为自然流畅的语音。

```java
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.request.SpeechRequest;
import java.io.File;

public class TTSDemo {
    public static void main(String[] args) {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        SpeechRequest request = SpeechRequest.builder()
                .model("gpt-4o-mini-tts-2025-12-15")  // 或 "tts-1", "tts-1-hd"
                .input("你好，欢迎使用 Fast OpenAI SDK！这是一个文本转语音的演示。")
                .voice("alloy")                       // 可选: alloy, echo, fable, onyx, nova, shimmer
                .response_format("mp3")               // 可选: mp3, opus, aac, flac
                .speed(1.0)                           // 语速: 0.25 ~ 4.0，默认 1.0
                .build();

        // 生成语音文件到指定路径
        String outputPath = "output_speech.mp3";
        File audioFile = service.speech(request, outputPath);
        System.out.println("语音文件已生成: " + audioFile.getAbsolutePath());
    }
}
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `model` | String | ✅ | TTS 模型，支持 `tts-1`, `tts-1-hd`, `gpt-4o-mini-tts-2025-12-15` |
| `input` | String | ✅ | 要转换为语音的文本，最多 4096 个字符 |
| `voice` | String | ✅ | 语音类型：`alloy`, `echo`, `fable`, `onyx`, `nova`, `shimmer` |

---

### 7. 语音转文本 (Whisper ASR)

SDK 支持 OpenAI Whisper 模型的语音识别能力，可将音频文件转换为文本。

```java
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.request.AudioTextRequest;
import com.kylin.fast.openai.result.WhisperResult;
import java.io.File;

public class WhisperDemo {
    public static void main(String[] args) {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        // 准备音频文件 (支持 mp3, mp4, mpeg, mpga, m4a, wav, webm 格式)
        File audioFile = new File("output_speech.mp3");

        AudioTextRequest request = AudioTextRequest.builder()
                .model("whisper-1")                   // 语音识别模型
                .responseFormat("text")               // 可选: json, text, srt, verbose_json, vtt
                .language("zh")                       // 指定语言 ISO-639-1 编码 (如 zh, en, ja)
                .prompt("")                           // 可选提示词，引导识别风格
                .temperature(0.0)                     // 采样温度，默认 0
                .build();

        WhisperResult result = service.whisper(request, audioFile);
        System.out.println("识别结果: " + result.getText());
    }
}
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `model` | String | ✅ | 模型名称，目前仅支持 `whisper-1` |
| `responseFormat` | String | ❌ | 输出格式：`json`(默认), `text`, `srt`, `verbose_json`, `vtt` |
| `language` | String | ❌ | 音频语言 ISO-639-1 编码，如 `zh`(中文), `en`(英文), `ja`(日文) |
| `prompt` | String | ❌ | 提示词，用于引导模型识别风格或继续之前的音频 |
| `temperature` | Double | ❌ | 采样温度，范围 `0.0` ~ `1.0`，默认 `0.0` |

---

### 8. Graph Agent 智能体编排

> 🆕 **全新能力**：基于 `StateGraph` 有向状态图引擎构建的 AI Agent 编排框架。几行 Java 代码即可实现 ReAct（推理-行动）循环、并行工具调度、流式 Token 透传、以及会话级 Checkpoint 持久化与中断恢复。

#### 8.1 StateGraph 基础用法

`StateGraph` 是一个轻量级的有向状态图引擎。你只需定义一个状态 POJO、注册节点、连接边，即可编译运行：

```java
import com.kylin.fast.graph.core.*;
import java.util.*;

public class SimpleGraphDemo {
    // 1. 定义状态 POJO（纯 Java Bean，零框架侵入）
    public static class FlowState {
        private String data = "";
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    public static void main(String[] args) {
        // 2. 构建图
        StateGraph<FlowState> graph = new StateGraph<>(FlowState::new);

        graph.addNode("nodeA", state -> {
            Map<String, Object> update = new HashMap<>();
            update.put("data", "Hello");
            return update;
        });

        graph.addNode("nodeB", state -> {
            Map<String, Object> update = new HashMap<>();
            update.put("data", state.getData() + " World!");
            return update;
        });

        // 3. 连线：START → nodeA → nodeB → END
        graph.addEdge(StateGraph.START, "nodeA");
        graph.addEdge("nodeA", "nodeB");
        graph.addEdge("nodeB", StateGraph.END);

        // 4. 编译并执行
        CompiledGraph<FlowState> compiled = graph.compile(new InMemorySaver());
        FlowState result = compiled.invoke(new FlowState(),
                GraphConfig.builder().threadId("demo-01").build());

        System.out.println(result.getData()); // 输出: Hello World!
    }
}
```

**核心概念：**

| 概念 | 说明 |
|------|------|
| `StateGraph<S>` | 有向状态图容器，管理节点、边、Reducer 注册 |
| `Node<S>` | 函数式接口 `(S) -> Map<String, Object>`，接收状态返回增量更新 |
| `Reducer` | 自定义字段合并策略（默认为覆盖），常用于 List 追加 |
| `CompiledGraph<S>` | 编译后的可执行图实例，提供 `invoke()` / `stream()` |
| `ConditionalEdge` | 基于状态运行时的条件路由，实现动态分支跳转 |

#### 8.2 ReAct Agent 实战

ReAct (Reasoning + Acting) 是 AI Agent 的核心模式：LLM 推理 → 决定调用工具 → 并行执行工具 → 回传结果 → 再推理 → ... → 最终回复。使用 `OpenAiAgentNode` 和 `OpenAiActionNode`，两行代码即可完成：

```
   ┌─────────┐     有 ToolCalls     ┌──────────┐
   │  agent  │ ─────────────────→  │  action   │
   │ (LLM)   │ ←─────────────────  │ (并发工具) │
   └─────────┘     执行完成后返回    └──────────┘
        │
        │ 无 ToolCalls (最终回复)
        ▼
     __end__
```

**完整示例：天气查询 + 数学计算 Agent**

```java
import com.kylin.fast.graph.core.*;
import com.kylin.fast.graph.model.ChatMessage;
import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;
import java.util.*;

public class ReActAgentDemo {

    // ==================== 1. 定义 Agent 状态 ====================
    public static class AgentState {
        private List<ChatMessage> messages = new ArrayList<>();
        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    }

    // ==================== 2. 定义工具类 (@AiFunction 注解) ====================
    public static class MyTools {

        @AiFunction(name = "get_current_weather", description = "获取指定城市的当前天气情况")
        public static String getCurrentWeather(
                @AiFunctionParam(name = "location", description = "城市名称，例如：北京、上海") String location,
                @AiFunctionParam(name = "unit", description = "温度单位：celsius 或 fahrenheit", required = false) String unit
        ) {
            if (location.contains("北京")) return "北京今天是晴天，气温 25°C。";
            if (location.contains("上海")) return "上海今天是阴天，有小雨，气温 22°C。";
            return location + " 的天气是多云，气温 20°C。";
        }

        @AiFunction(name = "calculator", description = "执行基本的数学运算：加法、减法、乘法、除法")
        public static int calculator(
                @AiFunctionParam(name = "a", description = "第一个数字") int a,
                @AiFunctionParam(name = "b", description = "第二个数字") int b,
                @AiFunctionParam(name = "operation", description = "运算符：add, subtract, multiply, divide") String operation
        ) {
            switch (operation) {
                case "add":      return a + b;
                case "subtract": return a - b;
                case "multiply": return a * b;
                case "divide":   return a / b;
                default: throw new IllegalArgumentException("不支持的运算: " + operation);
            }
        }
    }

    // ==================== 3. 构建 ReAct Agent ====================
    public static void main(String[] args) {
        OpenAiService service = new OpenAiService(OpenAiConfig.loadFromProperties());

        // 3.1 构建 StateGraph，注册 Reducer（messages 追加模式）
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
        graph.registerReducer("messages", (oldVal, newVal) -> {
            List<ChatMessage> merged = new ArrayList<>();
            if (oldVal != null) merged.addAll((Collection<ChatMessage>) oldVal);
            if (newVal != null) merged.addAll((Collection<ChatMessage>) newVal);
            return merged;
        });

        // 3.2 编译图 & 注册工具
        CompiledGraph<AgentState> compiled = graph.compile(new InMemorySaver())
                .registerTools(new MyTools());  // 🌟 一行注册所有 @AiFunction 工具

        // 3.3 添加 agent 节点（LLM 决策）和 action 节点（并行工具执行）
        graph.addNode("agent", OpenAiAgentNode.create(service, "gpt-4o", compiled)::apply);
        graph.addNode("action", OpenAiActionNode.create(compiled)::apply);

        // 3.4 编排拓扑
        graph.addEdge(StateGraph.START, "agent");
        graph.addConditionalEdges("agent",
                state -> {
                    List<ChatMessage> msgs = state.getMessages();
                    ChatMessage last = msgs.get(msgs.size() - 1);
                    // 有工具调用 → action，否则 → 结束
                    return (last.getToolCalls() != null && !last.getToolCalls().isEmpty())
                            ? "action" : "end";
                },
                new HashMap<String, String>() {{
                    put("action", "action");
                    put("end", StateGraph.END);
                }});
        graph.addEdge("action", "agent"); // 工具结果返回 agent 继续推理

        // 3.5 一键执行
        AgentState state = new AgentState();
        state.getMessages().add(ChatMessage.user(
                "请帮我同时查询北京和上海的天气。拿到两地气温数字后，帮我把它们相加求和！"));

        AgentState result = compiled.invoke(state,
                GraphConfig.builder().threadId("react-session-01").build());

        // 打印完整对话历史
        for (ChatMessage msg : result.getMessages()) {
            System.out.println("[" + msg.getRole() + "]: " + msg.getContent());
        }
    }
}
```

**关键设计点：**

- **`OpenAiAgentNode`**：自动将 `AgentState.messages` 转换为 SDK 的 `BaseMessage` 发送给 LLM，并注入已注册的工具声明。LLM 返回的 `tool_calls` / 最终回复自动写回状态。
- **`OpenAiActionNode`**：使用 **`parallelStream()` 多线程高并发** 执行 LLM 下达的所有工具调用，结果自动聚合为 `tool` 角色消息写回状态。
- **`registerTools()`**：传入 `new MyTools()` 实例，SDK 通过反射自动解析 `@AiFunction` 注解生成 `GptTool` 元数据和调用句柄。
- **`addConditionalEdges()`**：基于状态运行时的动态路由 — 检测最后一条消息是否有 `toolCalls`，决定下一站是 action 还是结束。

#### 8.3 流式执行与 Token 透传

Graph 执行支持全链路流式事件。通过 `stream()` 方法获取 `Iterator<GraphEvent>`，实时接收节点生命周期事件和 LLM Token 增量：

```java
Iterator<GraphEvent> stream = compiled.stream(state,
        GraphConfig.builder().threadId("stream-demo").build());

while (stream.hasNext()) {
    GraphEvent event = stream.next();
    switch (event.getType()) {
        case "node_start":
            System.out.println("▶ 进入节点: " + event.getNodeName());
            break;
        case "token":
            // 🌟 大模型流式输出的每个增量 Token
            System.out.print((String) event.getPayload());
            break;
        case "node_end":
            System.out.println("\n✔ 节点完成: " + event.getNodeName());
            break;
    }
}
```

`GraphEvent` 类型说明：

| 事件类型 | 含义 | Payload |
|---------|------|---------|
| `node_start` | 节点开始执行 | `null` |
| `token` | LLM 流式 Token 增量 | 文本片段 (`String`) |
| `node_end` | 节点执行完成 | 当前完整状态快照 |
| `error` | 执行异常 | 错误信息 (`String`) |

在自定义节点中，可通过 `GraphContext.getEmitter()` 获取 `StreamEmitter`，将任意事件（如 Token）实时透传给调用方：

```java
graph.addNode("my_node", state -> {
    StreamEmitter emitter = GraphContext.getEmitter();
    if (emitter != null) {
        emitter.emit(new GraphEvent("token", "my_node", "这是实时透传的 Token 片段"));
    }
    // ... 正常业务逻辑
    return updateMap;
});
```

#### 8.4 Checkpoint 持久化与中断恢复

Graph 引擎内置三种开箱即用的 `StateSaver` 实现，支持会话状态的持久化、断点续跑和人工审批（Human-in-the-Loop）模式：

**① InMemorySaver（内存存储，适合测试）**

```java
StateSaver saver = new InMemorySaver();
CompiledGraph<MyState> compiled = graph.compile(saver);
```

**② FileStateSaver（磁盘持久化，断电不丢失）**

```java
// 自动在指定目录下创建 threadId 子目录，保存 JSON 格式的 Checkpoint
StateSaver saver = new FileStateSaver("./agent_checkpoints");
```

**③ CallbackStateSaver（Lambda 委托，对接任意存储）**

```java
// 零类编写，直接 Lambda 对接外部 Redis / 数据库 / 微服务
StateSaver saver = new CallbackStateSaver(
        (threadId, checkpoint) -> redis.set("agent:" + threadId, JSON.toJSONString(checkpoint)),
        threadId -> JSON.parseObject(redis.get("agent:" + threadId), Checkpoint.class)
);
```

**中断与恢复（Human-in-the-Loop）：**

通过 `GraphConfig.interruptBefore` 指定在哪些节点前挂起，等待人工审批后从 Checkpoint 恢复继续执行：

```java
// 第一步：带中断执行 — 在 "human_approval" 节点前自动挂起
GraphConfig config = GraphConfig.builder()
        .threadId("session-01")
        .interruptBefore(Collections.singletonList("human_approval"))
        .build();

MyState midState = compiled.invoke(new MyState(), config);
// 此时 human_approval 未被实际执行，状态和进度已持久化到 Checkpoint

// 第二步：恢复执行 — 相同 threadId，不带中断参数
GraphConfig resumeConfig = GraphConfig.builder()
        .threadId("session-01")  // 相同 threadId，自动从 Checkpoint 恢复
        .build();

MyState finalState = compiled.invoke(new MyState(), resumeConfig);
// 从 human_approval 节点继续执行直到 END
```

**Checkpoint 数据模型：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `threadId` | String | 会话唯一标识 |
| `checkpointId` | String | 检查点唯一 ID（自动生成） |
| `stateSnapshot` | Map\<String, Object\> | 当前状态完整快照 |
| `nextNode` | String | 下一个待执行的节点名 |
| `createdAt` | long | 创建时间戳（毫秒） |

---

## 🗂️ 核心包结构与架构说明

- **`com.kylin.fast.openai.api`**: 基于 Retrofit 封装的核心网络调用服务层。
- **`com.kylin.fast.openai.config`**: SDK 全局配置管理器，单例模式维护多级代理以及灵活的负载均衡策略。
- **`com.kylin.fast.openai.request / result`**: 对应模型 API 的 HTTP DTO 传输与解析模型定义。
- **`com.kylin.fast.openai.stream`**: 依托 OkHttp EventSource 底层 API 打造的流式响应调度处理中心。
- **`com.kylin.fast.openai.function`**: `@AiFunction` 注解的核心解析包与 `FunctionContextHandler` 上下文拦截逻辑。
- **`com.kylin.fast.openai.interceptor`**: 智能拦截器矩阵，用于请求头的动态注入（Bearer Token 轮询）、异常捕获、以及重试调度。
- **`com.kylin.fast.jina`**: Jina AI 生态系统的专门接入点。
- **`com.kylin.fast.graph.core`**: 🆕 **Graph Agent 核心引擎** — `StateGraph` 状态图容器、`CompiledGraph` 编译执行器、`OpenAiAgentNode` LLM 决策节点、`OpenAiActionNode` 并行工具节点、`Checkpoint` 持久化模型、三种 `StateSaver` 实现。
- **`com.kylin.fast.graph.model`**: 🆕 **Agent 消息模型** — `ChatMessage`（支持多模态图片）、`ToolCall`、`ChatMessageChunk`、`ToolCallChunk`。
- **`com.kylin.fast.graph.provider`**: 🆕 **可插拔模型适配层** — `ChatModel` 接口、`Tool` 接口、`ModelOptions` 配置、`ToolDefinition` 元数据。

---

## 🤝 参与贡献

我们欢迎社区开发者提交 Issue 和 Pull Request，共同参与完善此开源生态！
- 在提交 PR 前，请确保代码**向后完全兼容 Java 8**。
- 请尽可能为公共方法或接口更新详细的 Javadoc，并附带通过测试用例。

## 📄 许可证 (License)

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 协议开源，请自由使用、分发与修改。
