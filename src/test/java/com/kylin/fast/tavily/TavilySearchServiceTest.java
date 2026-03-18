package com.kylin.fast.tavily;

import com.kylin.fast.openai.config.OpenAiConfig;
import com.kylin.fast.tavily.api.TavilySearchService;
import com.kylin.fast.tavily.request.TavilySearchRequest;
import com.kylin.fast.tavily.response.TavilySearchResponse;
import com.kylin.fast.tavily.response.TavilySearchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TavilySearchServiceTest {

    private static TavilySearchService tavilySearchService;

    @BeforeAll
    public static void init() {
        OpenAiConfig config = OpenAiConfig.loadFromProperties();
        tavilySearchService = new TavilySearchService(config);
    }

    @Test
    public void testSimpleSearch() {
        String query = "小米股票";
        System.out.println("正在搜索: " + query);

        try {
            TavilySearchResponse response = tavilySearchService.search(query);

            System.out.println("--- Search Results Start ---");
            System.out.println("Query: " + response.getQuery());
            System.out.println("Response Time: " + response.getResponseTime());
            System.out.println("Results Count: " + (response.getResults() != null ? response.getResults().size() : 0));
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                System.out.println("First Result Title: " + response.getResults().get(0).getTitle());
                System.out.println("First Result URL: " + response.getResults().get(0).getUrl());
            }
            System.out.println("--- Search Results End ---");

            Assertions.assertNotNull(response);
            Assertions.assertNotNull(response.getResults());
            Assertions.assertFalse(response.getResults().isEmpty());

        } catch (IOException e) {
            e.printStackTrace();
            Assertions.fail("请求失败: " + e.getMessage());
        }
    }

    @Test
    public void testAdvancedSearch() {
        String query = "ai";
        System.out.println("正在高级搜索: " + query);

        try {
            TavilySearchRequest request = TavilySearchRequest.builder()
                    .query(query)
                    .searchDepth("advanced")
                    .maxResults(3)
                    .includeAnswer("basic")
                    .includeImages(true)
                    .build();

            TavilySearchResponse response = tavilySearchService.search(request);

            System.out.println("--- Advanced Search Results Start ---");
            System.out.println("Query: " + response.getQuery());
            System.out.println("Answer: " + response.getAnswer());
            System.out.println("Images Count: " + (response.getImages() != null ? response.getImages().size() : 0));
            System.out.println("Results Count: " + (response.getResults() != null ? response.getResults().size() : 0));
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                System.out.println("First Result Title: " + response.getResults().get(0).getTitle());
                System.out.println("First Result URL: " + response.getResults().get(0).getUrl());
            }
            System.out.println("--- Advanced Search Results End ---");

            Assertions.assertNotNull(response);
            Assertions.assertEquals(query, response.getQuery());

        } catch (IOException e) {
            e.printStackTrace();
            Assertions.fail("请求失败: " + e.getMessage());
        }
    }
}
