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
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            agentProcess = pb.start();
            int port = parsePort(agentProps.getBaseUrl());
            log.info("");
            log.info("╔══════════════════════════════════════════════════╗");
            log.info("║        Python Agent 自动启动成功                ║");
            log.info("║  PID    : {}                ║", agentProcess.pid());
            log.info("║  Port   : {}                              ║", port);
            log.info("║  URL    : http://localhost:{}/agent/search     ║", port);
            log.info("║  Status : 运行中                              ║");
            log.info("╚══════════════════════════════════════════════════╝");
            log.info("");
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
