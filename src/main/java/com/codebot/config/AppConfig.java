package com.codebot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GitHubProperties.class, OllamaProperties.class})
public class AppConfig {
}
