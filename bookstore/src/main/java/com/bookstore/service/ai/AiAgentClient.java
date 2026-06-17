package com.bookstore.service.ai;

import com.bookstore.config.AiAgentProperties;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAgentClient {

    private final AiAgentProperties agentProps;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = agentProps.getTimeoutSeconds() * 1000;
        factory.setConnectTimeout(Math.min(timeoutMs, 10000));
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public AgentChatResponse chat(Long userId, String message, Long sessionId,
                                  List<Map<String, Object>> history,
                                  List<Map<String, Object>> bookCandidates) {
        String url = trimSlash(agentProps.getBaseUrl()) + "/agent/chat";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", userId);
        body.put("session_id", sessionId);
        body.put("message", message);
        body.put("history", history);
        body.put("book_candidates", bookCandidates);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasText(agentProps.getApiKey())) {
            headers.set("X-API-Key", agentProps.getApiKey());
        }

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            return new AgentChatResponse(
                    root.path("reply").asText(),
                    parseLongList(root.path("referenced_book_ids"))
            );
        } catch (RestClientResponseException ex) {
            log.warn("Agent service error status={}", ex.getStatusCode());
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI Agent 服务调用失败");
        } catch (Exception ex) {
            log.warn("Agent service unreachable", ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI Agent 服务暂不可用");
        }
    }

    private List<Long> parseLongList(JsonNode node) {
        List<Long> ids = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) {
                if (n.canConvertToLong()) ids.add(n.asLong());
            }
        }
        return ids;
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    public record AgentChatResponse(String reply, List<Long> referencedBookIds) {}

    public AgentSearchResponse search(String keyword, List<Map<String, Object>> candidates,
                                      Long userId, String userProfile) {
        String url = trimSlash(agentProps.getBaseUrl()) + "/agent/search";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("keyword", keyword);
        body.put("candidates", candidates);
        if (userId != null) {
            body.put("user_id", userId);
        }
        if (hasText(userProfile)) {
            body.put("user_profile", userProfile);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasText(agentProps.getApiKey())) {
            headers.set("X-API-Key", agentProps.getApiKey());
        }

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            return new AgentSearchResponse(
                parseLongList(root.path("book_ids")),
                parseStringList(root.path("reasons"))
            );
        } catch (Exception ex) {
            log.warn("Agent search error", ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI Agent 搜索服务暂不可用");
        }
    }

    public AgentRecommendResponse recommend(Long userId, String userProfile,
                                            List<Map<String, Object>> candidates, int limit) {
        String url = trimSlash(agentProps.getBaseUrl()) + "/agent/recommend";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", userId);
        body.put("user_profile", userProfile);
        body.put("candidates", candidates);
        body.put("limit", limit);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasText(agentProps.getApiKey())) {
            headers.set("X-API-Key", agentProps.getApiKey());
        }

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            return new AgentRecommendResponse(
                parseLongList(root.path("book_ids")),
                parseStringList(root.path("reasons"))
            );
        } catch (Exception ex) {
            log.warn("Agent recommend error", ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI Agent 推荐服务暂不可用");
        }
    }

    public record AgentSearchResponse(List<Long> bookIds, List<String> reasons) {}
    public record AgentRecommendResponse(List<Long> bookIds, List<String> reasons) {}

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) {
                list.add(n.asText());
            }
        }
        return list;
    }
}
