package com.bookstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bookstore.ai")
public class AiProperties {

    private boolean enabled = true;
    private String apiBase = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String apiKey;
    private String model = "qwen-plus";
    private String searchModel = "qwen-turbo";
    private String recommendModel = "qwen-turbo";

    private String searchApiBase;
    private String searchApiKey;
    private String recommendApiBase;
    private String recommendApiKey;
    private Double temperature = 0.7;
    private Integer maxTokens = 1024;
    private Integer timeoutSeconds = 30;
    private Integer historyLimit = 10;
    private String systemPrompt =
        "你是一家智能书店的助理小书。语气温和、专业、简洁。" +
        "可以为用户推荐图书、解释内容、回答购物相关问题。" +
        "回答时如果引用图书,请使用书名号《》并尽量贴近用户提供的图书目录。";

    private String keywordExtractionPrompt =
        "从用户的搜索意图中提取关键词，用于图书数据库搜索。" +
        "只返回关键词，用逗号分隔，不要解释。提取书名、作者、分类、主题。" +
        "示例输入: '推荐一本科幻小说' → 科幻\n" +
        "示例输入: '三体的作者还有什么作品' → 刘慈欣\n" +
        "示例输入: '有没有关于Java编程的书' → Java,编程";

    private String searchSystemPrompt =
        "你是智能书店的搜索专家。用户会用自然语言描述想找的图书。" +
        "我会提供一批候选图书，你需要从中选出最符合用户意图的图书。" +
        "只返回图书ID，格式为纯数字列表，用逗号分隔，不要任何解释。" +
        "如果候选图书都不符合，返回 empty。" +
        "示例输出: 101,205,312";

    private String recommendSystemPrompt =
        "你是智能书店的个性化推荐专家。根据用户的阅读偏好和候选图书，" +
        "为用户推荐最合适的图书。只返回图书ID，格式为纯数字列表，用逗号分隔，" +
        "不要任何解释。优先推荐用户可能感兴趣但还没看过的书。" +
        "示例输出: 101,205,312";
}
