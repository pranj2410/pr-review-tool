package com.codebot.service;

import com.codebot.config.OllamaProperties;
import com.codebot.model.OllamaGenerateRequest;
import com.codebot.model.OllamaGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class OllamaService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    private static final String PROMPT_TEMPLATE = """
            You are an expert code reviewer. Analyse the following git diff and provide a concise review covering:
            1. Security vulnerabilities (hardcoded secrets, injection risks, insecure dependencies)
            2. Code quality issues (naming, duplication, complexity)
            3. Best practice violations (error handling, logging, design patterns)

            Be specific - reference line numbers where possible. Keep the tone constructive.
            Format the response in clean markdown so it renders well as a GitHub PR comment.

            Git diff:
            %s
            """;

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;

    public OllamaService(WebClient.Builder webClientBuilder, OllamaProperties ollamaProperties) {
        this.ollamaProperties = ollamaProperties;
        this.webClient = webClientBuilder
                .baseUrl(ollamaProperties.getBaseUrl())
                .build();
    }

    public String review(String diff) {
        String prompt = PROMPT_TEMPLATE.formatted(diff);
        OllamaGenerateRequest request = new OllamaGenerateRequest(ollamaProperties.getModel(), prompt, false);

        OllamaGenerateResponse response = webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaGenerateResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .retryWhen(Retry.max(0))
                .block();

        return response != null ? response.getResponse() : null;
    }
}
