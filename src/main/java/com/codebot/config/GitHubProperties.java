package com.codebot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    private String token;
    private String webhookSecret;
}
