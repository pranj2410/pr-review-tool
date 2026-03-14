package com.codebot.service;

import com.codebot.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String FALLBACK_COMMENT = "⚠️ Code review bot encountered an error. Please review manually.";

    private final GitHubService gitHubService;
    private final OllamaService ollamaService;

    @Async("reviewTaskExecutor")
    public void handleAsync(PullRequestEvent event) {
        if (event == null || event.getPullRequest() == null || event.getRepository() == null) {
            log.warn("Skipping review because webhook event payload is incomplete");
            return;
        }

        String action = event.getAction();
        if (!"opened".equals(action) && !"synchronize".equals(action) && !"edited".equals(action) && !"reopened".equals(action)) {
            log.info("Ignoring pull request action {}", action);
            return;
        }

        int prNumber = event.getPullRequest().getNumber();
        String repoFullName = event.getRepository().getFullName();

        try {
            String diff = gitHubService.fetchDiff(event.getPullRequest().getDiffUrl());
            if (diff == null || diff.isBlank()) {
                log.info("Primary diff URL returned empty for {}/pull/{}, falling back to GitHub pull request API", repoFullName, prNumber);
                diff = gitHubService.fetchPullRequestDiff(repoFullName, prNumber);
            }

            if (diff == null || diff.isBlank()) {
                log.warn("Skipping review for {}/pull/{} because the diff is empty", repoFullName, prNumber);
                return;
            }

            String reviewComment = ollamaService.review(diff);
            if (reviewComment == null || reviewComment.isBlank()) {
                log.warn("Ollama returned an empty review for {}/pull/{}", repoFullName, prNumber);
                gitHubService.postComment(repoFullName, prNumber, FALLBACK_COMMENT);
                return;
            }

            gitHubService.postComment(repoFullName, prNumber, reviewComment);
        } catch (Exception exception) {
            log.error("Automated review failed for {}/pull/{}", repoFullName, prNumber, exception);
            gitHubService.postComment(repoFullName, prNumber, FALLBACK_COMMENT);
        }
    }
}
