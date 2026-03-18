package com.kylin.fast.tavily.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TavilySearchResult {

    private String title;

    private String url;

    private String content;

    private Double score;

    @JsonProperty("published_date")
    private String publishedDate;

    @JsonProperty("raw_content")
    private String rawContent;

    private String favicon;
}
