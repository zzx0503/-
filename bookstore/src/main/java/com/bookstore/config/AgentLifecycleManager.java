package com.bookstore.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLifecycleManager {

    private final AiAgentProperties agentProps;
    private Process agentProcess;

    @PostConstruct
    public void startAgent() {
        if (!agentProps.isAutoStart() || !agentProps.isEnabled()) {
            return;
        }

        String projectPath = agentProps.getProjectPath();
        File venvPython = new File(projectPath, "venv/Scripts/python.exe");
        if (!venvPython.exists()) {
            log.warn("[Agent启动] Python venv 不存在: {}, 跳过自动启动", venvPython.getAbsolutePath());
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                venvPython.getAbsolutePath(),
                "-m", "uvicorn", "app.main:app",
                "--host", "0.0.0.0",
                "--port", String.valueOf(parsePort(agentProps.getBaseUrl()))
            );
            pb.directory(new File(projectPath));
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            agentProcess = pb.start();
            log.info("[Agent启动] Python Agent 已启动, PID={}, port={}",
                agentProcess.pid(), parsePort(agentProps.getBaseUrl()));
        } catch (IOException e) {
            log.error("[Agent启动] 启动失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopAgent() {
        if (agentProcess != null && agentProcess.isAlive()) {
            agentProcess.destroyForcibly();
            log.info("[Agent停止] Python Agent 已关闭");
        }
    }

    private static int parsePort(String url) {
        try {
            return Integer.parseInt(url.replaceAll(".*:(\\d+).*", "$1"));
        } catch (Exception e) {
            return 8000;
        }
    }
}
