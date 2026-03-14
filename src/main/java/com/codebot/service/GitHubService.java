package com.codebot.service;

import com.codebot.config.GitHubProperties;
import com.codebot.model.GitHubCommentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class GitHubService {

    private static final int DIFF_MAX_LENGTH = 8000;
    private static final String DIFF_TRUNCATION_NOTE = "\n[Diff truncated for context window]";

    private final WebClient webClient;

    public GitHubService(WebClient.Builder webClientBuilder, GitHubProperties gitHubProperties) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubProperties.getToken())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                .build();
    }

    public String fetchDiff(String diffUrl) {
        if (diffUrl == null || diffUrl.isBlank()) {
            return "";
        }

        String diff = webClient.get()
                .uri(diffUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return normalizeDiff(diff);
    }

    public String fetchPullRequestDiff(String repoFullName, int prNumber) {
        String[] repoCoordinates = splitRepoFullName(repoFullName);
        String diff = webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", repoCoordinates[0], repoCoordinates[1], prNumber)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return normalizeDiff(diff);
    }

    private String normalizeDiff(String diff) {
        if (diff == null) {
            return "";
        }

        if (diff.length() > DIFF_MAX_LENGTH) {
            return diff.substring(0, DIFF_MAX_LENGTH) + DIFF_TRUNCATION_NOTE;
        }
        return diff;
    }

    public void postComment(String repoFullName, int prNumber, String body) {
        String[] repoCoordinates = splitRepoFullName(repoFullName);
        GitHubCommentRequest request = new GitHubCommentRequest(body);
        Mono<Void> commentCall = webClient.post()
                .uri("/repos/{owner}/{repo}/issues/{prNumber}/comments", repoCoordinates[0], repoCoordinates[1], prNumber)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);

        try {
            commentCall.block();
            log.info("Posted review comment to {}/pull/{}", repoFullName, prNumber);
        } catch (Exception exception) {
            log.error("Failed to post review comment to {}/pull/{}", repoFullName, prNumber, exception);
        }
    }

    private String[] splitRepoFullName(String repoFullName) {
        String[] parts = repoFullName.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Invalid repository full name: " + repoFullName);
        }
        return parts;
    }
}
