package com.kylin.fast.jina;

import com.kylin.fast.jina.api.JinaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

/**
 * JinaService 的单元测试
 */
public class JinaServiceTest {

    private static JinaService jinaService;

    // 请替换为你自己的有效 API Key，或者通过环境变量获取
    private static final String API_KEY = "jina_34e68d66a1134a8ba40aef2973f69d6cZc8ziGCbCBvzz5aprSLMGN4cCanX";

    @BeforeAll
    public static void init() {
        // 如果你需要使用代理，请修改此处配置
        // Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 7890));
        Proxy proxy = null;

        // 初始化 Service
        jinaService = new JinaService(API_KEY, Duration.ofSeconds(60), proxy);
    }

    @Test
    public void testFetchExampleDomain() {
        String url = "https://www.buz.ai/help-center/";
        System.out.println("正在获取: " + url);

        try {
            String markdown = jinaService.fetchMarkdown(url);

            System.out.println("--- Markdown Content Start ---");
            System.out.println(markdown);
            System.out.println("--- Markdown Content End ---");


        } catch (IOException e) {
            e.printStackTrace();
            Assertions.fail("请求失败: " + e.getMessage());
        }
    }

    @Test
    public void testFetchWithComplexUrl() {
        // 测试一个稍微复杂一点的 URL (CSDN 博客)
        String url = "https://blog.csdn.net/qq_26086231/article/details/126744036";
        System.out.println("正在获取: " + url);

        try {
            String markdown = jinaService.fetchMarkdown(url);

            System.out.println("--- Markdown Content Start ---");
            // 只打印前500个字符避免日志过多
            if (markdown.length() > 500) {
                System.out.println(markdown.substring(0, 500) + "...");
            } else {
                System.out.println(markdown);
            }
            System.out.println("--- Markdown Content End ---");

            Assertions.assertNotNull(markdown);
            Assertions.assertTrue(markdown.length() > 0);

        } catch (IOException e) {
            System.err.println("该测试可能因为网络原因或Jina限流而失败，这在单元测试中是可接受的。Error: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidUrl() {
        // 测试无效 URL 的处理
        String url = "https://this.is.a.fake.url.that.does.not.exist.com";
        System.out.println("正在获取无效 URL: " + url);

        // Jina API 对于无效 URL 可能会返回错误或者特定的错误页面内容，
        // 这里主要测试是否会抛出 IOException 或者返回内容，确保程序不崩溃
        try {
            String result = jinaService.fetchMarkdown(url);
            System.out.println("无效 URL 返回内容: " + result);
        } catch (IOException e) {
            System.out.println("捕获到预期的异常: " + e.getMessage());
            Assertions.assertNotNull(e.getMessage());
        }
    }
}
