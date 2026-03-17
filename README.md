# Fast OpenAI SDK

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://openjdk.java.net/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kylinshinlylin/fast-openai-sdk.svg)](https://central.sonatype.com/artifact/io.github.kylinshinlylin/fast-openai-sdk)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

**Fast OpenAI SDK** 是一个专为 Java 开发者设计的轻量级、高性能 SDK，旨在提供与 OpenAI 及 Jina API 完美兼容的客户端调用接口。它不仅具备极简的接入体验，还内置了流式响应（SSE）、多 API Key 负载均衡、请求重试、视觉能力以及大模型函数调用（Function Calling）等核心企业级特性。

## ✨ 核心特性

- **🚀 极致轻量与兼容**：**纯 Java 8 开发**，零 Spring Boot 等重量级依赖，无侵入性。可无缝集成到各类 Java 应用程序。
- **⚡ 高性能网络层**：底层基于业界成熟的 **Retrofit2 & OkHttp3** 构建，确保网络请求的高吞吐与低延时。
- **💬 完整兼容大模型 API**：全面支持文本生成（Chat）、打字机流式输出（SSE）、多模态视觉（Vision）、工具调用（Function Calling）等核心能力。
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
## 🗂️ 核心包结构与架构说明

- **`com.kylin.fast.openai.api`**: 基于 Retrofit 封装的核心网络调用服务层。
- **`com.kylin.fast.openai.config`**: SDK 全局配置管理器，单例模式维护多级代理以及灵活的负载均衡策略。
- **`com.kylin.fast.openai.request / result`**: 对应模型 API 的 HTTP DTO 传输与解析模型定义。
- **`com.kylin.fast.openai.stream`**: 依托 OkHttp EventSource 底层 API 打造的流式响应调度处理中心。
- **`com.kylin.fast.openai.function`**: `@AiFunction` 注解的核心解析包与 `FunctionContextHandler` 上下文拦截逻辑。
- **`com.kylin.fast.openai.interceptor`**: 智能拦截器矩阵，用于请求头的动态注入（Bearer Token 轮询）、异常捕获、以及重试调度。
- **`com.kylin.fast.jina`**: Jina AI 生态系统的专门接入点。

---

## 🤝 参与贡献

我们欢迎社区开发者提交 Issue 和 Pull Request，共同参与完善此开源生态！
- 在提交 PR 前，请确保代码**向后完全兼容 Java 8**。
- 请尽可能为公共方法或接口更新详细的 Javadoc，并附带通过测试用例。

## 📄 许可证 (License)

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 协议开源，请自由使用、分发与修改。
