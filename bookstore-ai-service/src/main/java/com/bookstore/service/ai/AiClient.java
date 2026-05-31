package com.bookstore.service.ai;

import com.bookstore.config.AiProperties;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final AiProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (props.getTimeoutSeconds() == null ? 30 : props.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(Math.min(timeoutMs, 10000));
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public String chatCompletion(List<ChatMsg> messages) {
        return chatCompletion(messages, props.getModel(), props.getApiBase(), props.getApiKey());
    }

    public String chatCompletion(List<ChatMsg> messages, String model) {
        return chatCompletion(messages, model, props.getApiBase(), props.getApiKey());
    }

    public String chatCompletion(List<ChatMsg> messages, String model, String apiBase, String apiKey) {
        String effectiveApiKey = StringUtils.hasText(apiKey) ? apiKey : props.getApiKey();
        String effectiveApiBase = StringUtils.hasText(apiBase) ? apiBase : props.getApiBase();
        if (!props.isEnabled() || !StringUtils.hasText(effectiveApiKey)) {
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI 服务未启用或未配置 apiKey");
        }
        String url = trimSlash(effectiveApiBase) + "/chat/completions";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", StringUtils.hasText(model) ? model : props.getModel());
        if (props.getTemperature() != null) body.put("temperature", props.getTemperature());
        if (props.getMaxTokens() != null) body.put("max_tokens", props.getMaxTokens());
        List<Map<String, String>> msgs = new ArrayList<>();
        for (ChatMsg m : messages) {
            Map<String, String> em = new LinkedHashMap<>();
            em.put("role", m.role());
            em.put("content", m.content());
            msgs.add(em);
        }
        body.put("messages", msgs);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveApiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("AI HTTP {} body={}", resp.getStatusCode(), resp.getBody());
                throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI 服务调用失败");
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choice0 = root.path("choices").path(0).path("message").path("content");
            String content = choice0.isMissingNode() || choice0.isNull() ? "" : choice0.asText();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI 返回内容为空");
            }
            return content.trim();
        } catch (RestClientResponseException ex) {
            log.warn("AI request failed status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI 服务调用失败: " + ex.getStatusCode());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI request error", ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI 服务暂不可用");
        }
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public record ChatMsg(String role, String content) {
        public static ChatMsg system(String c) { return new ChatMsg("system", c); }
        public static ChatMsg user(String c)   { return new ChatMsg("user", c); }
        public static ChatMsg assistant(String c) { return new ChatMsg("assistant", c); }
    }
}
