package com.kylin.fast.openai;

import com.kylin.fast.openai.api.OpenAiService;
import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.openai.request.ImageRequest;
import com.kylin.fast.openai.result.ImageResult;
import com.kylin.fast.openai.result.dto.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

/**
 * OpenAI Image Generation API 测试类
 * 文生图功能独立测试模块
 * <p>
 * 支持的模型：
 * - dall-e-2: 256x256, 512x512, 1024x1024
 * - dall-e-3: 1024x1024, 1792x1024, 1024x1792 (支持 quality 和 style 参数)
 * - gpt-image-1: 最新图像生成模型
 * <p>
 * Created by AI Assistant on 2025/03/18
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/images/create">OpenAI Image API</a>
 */
public class ImageGenerationTest {

    private static OpenAiService service;

    @BeforeAll
    public static void init() {
        System.out.println("[ImageGenerationTest] 初始化 OpenAiService...");
        // 从 src/main/resources/fast-openai.properties 自动读取配置，包含 apikey、proxy 等
        OpenAiConfig config = OpenAiConfig.loadFromProperties("fast-openai.properties");
        System.out.println("Loaded config: baseUrl=" + config.getBaseUrl() +
                ", keys=" + (config.getApiKeys() != null ? config.getApiKeys().size() : 0) +
                ", proxy=" + config.getProxy());

        // 如果文件不存在或者未配置 key，会采用默认实例化
        service = new OpenAiService(config);
    }

    /**
     * 测试 DALL-E 3 基础图像生成
     * 使用标准参数生成图像
     */
    @Test
    public void testCreateImageDalle3() {
        ImageRequest request = ImageRequest.builder()
                .prompt("A serene Japanese garden with cherry blossoms in spring, watercolor style")
                .model("dall-e-3")
                .size("1024x1024")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("DALL-E 3 Image Generated!");
        System.out.println("Created: " + result.getCreated());
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            for (Image image : result.getData()) {
                System.out.println("Image URL: " + image.getUrl());
            }
        }

        if (result.getUsage() != null) {
            System.out.println("Usage: " + result.getUsage());
        }
    }

    /**
     * 测试 DALL-E 3 HD 高质量模式
     * HD 模式生成更精细的图像
     */
    @Test
    public void testCreateImageDalle3HD() {
        ImageRequest request = ImageRequest.builder()
                .prompt("A futuristic cityscape at sunset with flying cars, cyberpunk style, highly detailed")
                .model("dall-e-3")
                .size("1024x1024")
                .quality("hd")
                .style("vivid")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("DALL-E 3 HD Image Generated!");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            result.getData().forEach(image -> System.out.println("Image URL: " + image.getUrl()));
        }
    }

    /**
     * 测试 DALL-E 3 Natural 自然风格
     * 更适合写实类图像
     */
    @Test
    public void testCreateImageDalle3Natural() {
        ImageRequest request = ImageRequest.builder()
                .prompt("A photorealistic portrait of an elderly craftsman working in a woodworking shop")
                .model("dall-e-3")
                .size("1024x1024")
                .quality("standard")
                .style("natural")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("DALL-E 3 Natural Style Image Generated!");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            result.getData().forEach(image -> {
                System.out.println("Image URL: " + image.getUrl());
            });
        }
    }

    /**
     * 测试 DALL-E 3 宽屏格式
     * 适合桌面壁纸或横幅
     */
    @Test
    public void testCreateImageDalle3Wide() {
        ImageRequest request = ImageRequest.builder()
                .prompt("Panoramic view of the Northern Lights over a snowy mountain landscape")
                .model("dall-e-3")
                .size("1792x1024")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("DALL-E 3 Wide Format Image Generated!");
        System.out.println("Size: 1792x1024");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            result.getData().forEach(image -> System.out.println("Image URL: " + image.getUrl()));
        }
    }

    /**
     * 测试 DALL-E 3 竖屏格式
     * 适合手机壁纸或海报
     */
    @Test
    public void testCreateImageDalle3Portrait() {
        ImageRequest request = ImageRequest.builder()
                .prompt("An elegant ballerina performing on stage with dramatic lighting")
                .model("dall-e-3")
                .size("1024x1792")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("DALL-E 3 Portrait Format Image Generated!");
        System.out.println("Size: 1024x1792");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            result.getData().forEach(image -> System.out.println("Image URL: " + image.getUrl()));
        }
    }

    /**
     * 测试 DALL-E 2 批量生成多张图像
     * DALL-E 2 支持一次生成最多 10 张图像
     */
    @Test
    public void testCreateImageDalle2Multiple() {
        ImageRequest request = ImageRequest.builder()
                .prompt("A cute corgi puppy playing in autumn leaves")
                .model("dall-e-2")
                .size("512x512")
                .n(4)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("DALL-E 2 Multiple Images Generated!");
        System.out.println("Requested: 4 images");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            int index = 0;
            for (Image image : result.getData()) {
                System.out.println("Image " + (++index) + " URL: " + image.getUrl());
            }
        }
    }

    /**
     * 测试 Base64 格式响应
     * 返回的图像数据为 Base64 编码
     */
    @Test
    public void testCreateImageBase64() {
        ImageRequest request = ImageRequest.builder()
                .prompt("A minimalist logo design for a coffee shop, warm colors")
                .model("dall-e-3")
                .size("1024x1024")
                .responseFormat("b64_json")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("Base64 Format Image Generated!");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            result.getData().forEach(image -> {
                if (image.getB64_json() != null) {
                    System.out.println("Base64 Data Length: " + image.getB64_json().length());
                    System.out.println("Base64 Data (first 100 chars): " + image.getB64_json().substring(0, Math.min(100, image.getB64_json().length())) + "...");
                }
            });
        }
    }

    /**
     * 测试 gpt-image-1 模型
     * OpenAI 最新的图像生成模型
     */
    @Test
    public void testCreateImageGptImage1() throws Exception {
        // 创建输出目录
        String outputDir = "src/test/resources/generated_images/";
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ImageRequest request = ImageRequest.builder()
                .prompt("A mystical forest with glowing mushrooms and fireflies at twilight")
                .model("gpt-image-1")
                .size("1024x1024")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("GPT-Image-1 Model Image Generated!");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null && !result.getData().isEmpty()) {
            Image image = result.getData().get(0);

            // 保存 Base64 图片到本地文件
            if (image.getB64_json() != null) {
                String fileName = outputDir + "gpt_image_1_" + System.currentTimeMillis() + ".png";
                saveBase64Image(image.getB64_json(), fileName);
                System.out.println("Image saved to: " + fileName);
                System.out.println("Base64 Data Length: " + image.getB64_json().length());
            }

            if (image.getUrl() != null) {
                System.out.println("Image URL: " + image.getUrl());
            }
        }

        if (result.getUsage() != null) {
            System.out.println("Usage: " + result.getUsage());
        }
    }

    /**
     * 辅助方法：将 Base64 字符串保存为图像文件
     *
     * @param base64Data Base64 编码的图像数据
     * @param fileName   保存的文件路径
     */
    private void saveBase64Image(String base64Data, String fileName) throws Exception {
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(imageBytes);
        }
    }

    /**
     * 测试带 user 标识的图像生成
     * 用于 OpenAI 监控和滥用检测
     */
    @Test
    public void testCreateImageWithUser() {
        ImageRequest request = ImageRequest.builder()
                .prompt("An abstract geometric pattern with blue and gold colors")
                .model("dall-e-3")
                .size("1024x1024")
                .user("user-test-12345")
                .n(1)
                .build();

        long startTime = System.currentTimeMillis();
        ImageResult result = service.createImage(request);
        long useTime = System.currentTimeMillis() - startTime;

        System.out.println("Image with User ID Generated!");
        System.out.println("User: user-test-12345");
        System.out.println("Use Time: " + useTime + " ms");

        if (result.getData() != null) {
            result.getData().forEach(image -> System.out.println("Image URL: " + image.getUrl()));
        }
    }
}
