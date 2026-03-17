package com.kylin.fast.openai.config;

import lombok.Data;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;

@Data
public class OpenAiConfig {
    private List<String> apiKeys;
    private String baseUrl = "https://api.openai.com/";
    private Duration timeout = Duration.ofSeconds(60);
    private Proxy proxy;
    private int maxRetries = 3;

    public static OpenAiConfig loadFromProperties() {
        return loadFromProperties("fast-openai.properties");
    }

    public static OpenAiConfig loadFromProperties(String filename) {
        OpenAiConfig config = new OpenAiConfig();
        try (InputStream input = OpenAiConfig.class.getClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                return config;
            }
            Properties prop = new Properties();
            prop.load(input);

            String keys = prop.getProperty("openai.api.keys");
            if (keys != null && !keys.isEmpty()) {
                config.setApiKeys(Arrays.asList(keys.split(",")));
            }

            String baseUrl = prop.getProperty("openai.api.baseUrl");
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.setBaseUrl(baseUrl);
            }

            String timeout = prop.getProperty("openai.api.timeout");
            if (timeout != null && !timeout.isEmpty()) {
                config.setTimeout(Duration.ofSeconds(Long.parseLong(timeout)));
            }

            String maxRetries = prop.getProperty("openai.api.maxRetries");
            if (maxRetries != null && !maxRetries.isEmpty()) {
                config.setMaxRetries(Integer.parseInt(maxRetries));
            }

            String proxyHost = prop.getProperty("openai.proxy.host");
            String proxyPort = prop.getProperty("openai.proxy.port");
            String proxyType = prop.getProperty("openai.proxy.type", "HTTP");
            if (proxyHost != null && proxyPort != null) {
                Proxy.Type type = Proxy.Type.valueOf(proxyType.toUpperCase());
                config.setProxy(new Proxy(type, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return config;
    }
}
