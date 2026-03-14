package com.codebot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequestEvent {

    private String action;
    @JsonProperty("pull_request")
    private PullRequest pullRequest;
    private Repository repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        private int number;
        private String title;
        @JsonProperty("diff_url")
        private String diffUrl;
        @JsonProperty("html_url")
        private String htmlUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        @JsonProperty("full_name")
        private String fullName;
    }
}
