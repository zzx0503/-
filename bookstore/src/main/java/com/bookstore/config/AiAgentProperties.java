package com.bookstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bookstore.ai.agent")
public class AiAgentProperties {

    private boolean enabled = false;
    private boolean autoStart = false;
    private String projectPath = "D:/IDEA/demo/ai-agent";
    private String baseUrl = "http://localhost:8000";
    private String apiKey;
    private int timeoutSeconds = 30;
    private boolean fallbackOnFailure = true;
    private int maxToolRounds = 5;
}
