package com.kylin.fast.jina;

import com.kylin.fast.jina.api.JinaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Properties;

/**
 * JinaService 的单元测试
 */
public class JinaServiceTest {

    private static JinaService jinaService;
    private static String apiKey;

    @BeforeAll
    public static void init() {
        // 读取 fast-openai.properties 配置文件
        Properties properties = new Properties();
        try (InputStream input = JinaServiceTest.class.getClassLoader().getResourceAsStream("fast-openai.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find fast-openai.properties");
                return;
            }
            properties.load(input);
            apiKey = properties.getProperty("jina.api.key");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Assertions.assertNotNull(apiKey, "Jina API Key cannot be null, please check fast-openai.properties");

        // 如果你需要使用代理，请修改此处配置
        // Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 7890));
        Proxy proxy = null;

        // 初始化 Service
        jinaService = new JinaService(apiKey, Duration.ofSeconds(60), proxy);
    }

    @Test
    public void testFetchExampleDomain() {
        String url = "https://www.qq.com/";
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

}
