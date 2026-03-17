package com.kylin.fast.openai;

import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.request.AudioTextRequest;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.SpeechRequest;
import com.kylin.fast.openai.request.dto.ImgMessage;
import com.kylin.fast.openai.request.dto.Message;
import com.kylin.fast.openai.result.ChatResult;
import com.kylin.fast.openai.result.WhisperResult;
import com.kylin.fast.openai.result.dto.ChatCompletionStreamChoice;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

public class OpenAiTest {

    private static OpenAiService service;

    @BeforeAll
    public static void init() {
        System.out.println("init");
        // 从 src/main/resources/fast-openai.properties 自动读取配置，包含 apikey、proxy 等
        OpenAiConfig config = OpenAiConfig.loadFromProperties("fast-openai.properties");
        System.out.println("Loaded config: baseUrl=" + config.getBaseUrl() +
                ", keys=" + (config.getApiKeys() != null ? config.getApiKeys().size() : 0) +
                ", proxy=" + config.getProxy());

        // 如果文件不存在或者未配置 key，会采用默认实例化
        service = new OpenAiService(config);
    }

    /**
     * 测试 Chat 非流式请求
     */
    @Test
    public void testChat() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(Message.of(MessageRole.user, "什么是人工智能？请用简短的话回答。"))
                .build();

        ChatResult result = service.createChat(request);
        System.out.println("Result: " + result.getChoices().get(0).getMessage().getContent());
        if (result.getUsage() != null) {
            System.out.println("Usage: " + result.getUsage());
        }
    }

    /**
     * 测试 Chat 流式请求
     */
    @Test
    public void testChatStream() throws InterruptedException {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(Message.of(MessageRole.user, "给我讲一个有趣的笑话。 markdown 格式返回"))
                .build();

        service.createChatStream(request, (result, isDone) -> {
            if (Objects.nonNull(result.getUsage())) {
                System.out.printf("usage=%s%n", result.getUsage());
            }
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                System.out.print(choice.getDelta().getContent());
                System.out.flush();
            }
        });

        // 等待流式输出完成
        Thread.sleep(5000);
    }

    /**
     * 测试 TTS (文本转语音)
     */
    @Test
    public void testTTS() {
        SpeechRequest request = SpeechRequest.builder()
                .model("gpt-4o-mini-tts-2025-12-15")
                .input("你好，这是一个文本转语音的测试。")
                .voice("alloy")
                .response_format("mp3")
                .build();

        String outputPath = "test_speech_output.mp3";
        File outputFile = service.speech(request, outputPath);
        System.out.println("Audio generated at: " + outputFile.getAbsolutePath());
    }

    /**
     * 测试 Whisper (语音转文本)
     */
    @Test
    public void testWhisper() {
        File file = new File("test_speech_output.mp3");
        if (!file.exists()) {
            System.out.println("No audio file found to transcribe. Please run testTTS first or provide a valid file.");
            return;
        }

        AudioTextRequest request = AudioTextRequest.builder()
                .model("whisper-1")
                .responseFormat("text")
                .build();

        long startTime = System.currentTimeMillis();
        WhisperResult result = service.whisper(request, file);
        long useTime = (System.currentTimeMillis() - startTime);

        System.out.println("Transcribed text: " + result.getText());
        System.out.println(String.format("useTime: %s ms", useTime));
    }

    /**
     * 测试图片交互 - 使用图片URL (非流式)
     * 使用 gpt-5.4 模型进行视觉理解
     */
    @Test
    public void testChatWithImageUrl() {
        // 使用示例图片URL
        String imageUrl = "https://pic.616pic.com/photoone/00/02/58/618cf527354c35308.jpg!/fw/1120";

        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(ImgMessage.of(
                        MessageRole.user,
                        "请描述这张图片的内容",
                        imageUrl))
                .build();

        ChatResult result = service.createChat(request);
        System.out.println("Image Analysis Result: " + result.getChoices().get(0).getMessage().getContent());
        if (result.getUsage() != null) {
            System.out.println("Usage: " + result.getUsage());
        }
    }

    /**
     * 测试图片交互 - 使用本地图片文件 (非流式)
     * 使用 gpt-5.4 模型进行视觉理解
     */
    @Test
    public void testChatWithImageFile() {
        File imageFile = new File("/Users/zengshilin/work/fast-openai-sdk/src/test/resources/img.png");
        if (!imageFile.exists()) {
            System.out.println("No image file found. Please provide a valid image file at: " + imageFile.getAbsolutePath());
            return;
        }

        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(ImgMessage.of(
                        MessageRole.user,
                        "请描述这张图片的内容",
                        imageFile))
                .build();

        ChatResult result = service.createChat(request);
        System.out.println("Image Analysis Result: " + result.getChoices().get(0).getMessage().getContent());
        if (result.getUsage() != null) {
            System.out.println("Usage: " + result.getUsage());
        }
    }

    /**
     * 测试图片交互 - 流式输出
     * 使用 gpt-5.4 模型进行视觉理解，支持流式响应
     */
    @Test
    public void testChatWithImageStream() throws InterruptedException {
        // 使用示例图片URL
        String imageUrl = "https://pic.616pic.com/photoone/00/02/58/618cf527354c35308.jpg!/fw/1120";

        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(ImgMessage.of(
                        MessageRole.user,
                        "请详细描述这张图片的内容，包括场景、颜色、氛围等",
                        imageUrl))
                .build();

        service.createChatStream(request, (result, isDone) -> {
            if (Objects.nonNull(result.getUsage())) {
                System.out.printf("usage=%s%n", result.getUsage());
            }
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                System.out.print(choice.getDelta().getContent());
                System.out.flush();
            }
        });

    }

    /**
     * 测试多图交互 - 同时发送多张图片
     * 使用 gpt-5.4 模型进行多图视觉理解
     */
    @Test
    public void testChatWithMultipleImages() {
        // 使用示例图片URLs
        String imageUrl1 = "https://pic.616pic.com/photoone/00/02/58/618cf527354c35308.jpg!/fw/1120";
        String imageUrl2 = "https://pic.616pic.com/photoone/00/06/02/618e27a728fd34751.jpg!/fw/1120";

        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(ImgMessage.of(MessageRole.user, "第一张图片:", imageUrl1))
                .addMessage(ImgMessage.of(MessageRole.user, "第二张图片:", imageUrl2))
                .addMessage(Message.of(MessageRole.user, "请比较这两张图片的异同点"))
                .build();

        ChatResult result = service.createChat(request);
        System.out.println("Multiple Images Analysis Result: " + result.getChoices().get(0).getMessage().getContent());
        if (result.getUsage() != null) {
            System.out.println("Usage: " + result.getUsage());
        }
    }

    /**
     * 测试图片交互 - 流式输出可视化交互版
     * 使用 gpt-5.4 模型进行视觉理解，带实时可视化输出和统计信息
     */
    @Test
    public void testChatWithImageUrlStreamVisual() throws InterruptedException {
        // 使用示例图片URLs
        String imageUrl1 = "https://pic.616pic.com/photoone/00/02/58/618cf527354c35308.jpg!/fw/1120";
        String imageUrl2 = "https://pic.616pic.com/photoone/00/06/02/618e27a728fd34751.jpg!/fw/1120";

        ChatRequest request = ChatRequest.builder()
                .model("gpt-5.4")
                .addMessage(ImgMessage.of(MessageRole.user, "第一张图片:", imageUrl1))
                .addMessage(ImgMessage.of(MessageRole.user, "第二张图片:", imageUrl2))
                .addMessage(Message.of(MessageRole.user, "请比较这两张图片的异同点"))
                .build();

        service.createChatStream(request, (result, isDone) -> {
            if (Objects.nonNull(result.getUsage())) {
                System.out.printf("usage=%s%n", result.getUsage());
            }
            for (ChatCompletionStreamChoice choice : result.getChoices()) {
                System.out.print(choice.getDelta().getContent());
                System.out.flush();
            }
        });

        // 等待流式输出完成
        Thread.sleep(10000);
    }
}
