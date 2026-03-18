package com.kylin.fast.tavily.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TavilySearchResponse {

    private String query;

    @JsonProperty("follow_up_questions")
    private List<String> followUpQuestions;

    private String answer;

    private List<String> images;

    private List<TavilySearchResult> results;

    @JsonProperty("response_time")
    private Double responseTime;

    @JsonProperty("request_id")
    private String requestId;
}
