package com.kylin.fast.tavily.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TavilySearchRequest {

    private String query;

    /**
     * news / finance / general
     */
    private String topic;

    /**
     * basic / advanced / fast / ultra-fast
     */
    @JsonProperty("search_depth")
    private String searchDepth;

    @JsonProperty("time_range")
    private String timeRange;

    @JsonProperty("include_images")
    private Boolean includeImages;

    /**
     * basic / advanced
     */
    @JsonProperty("include_answer")
    private String includeAnswer;

    /**
     * markdown / text / none
     */
    @JsonProperty("include_raw_content")
    private String includeRawContent;

    @JsonProperty("chunks_per_source")
    private Integer chunksPerSource;

    @JsonProperty("max_results")
    private Integer maxResults;

    @JsonProperty("include_domains")
    private List<String> includeDomains;

    @JsonProperty("exclude_domains")
    private List<String> excludeDomains;
}
