package com.codebot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OllamaGenerateRequest {
    private String model;
    private String prompt;
    private boolean stream;
}
