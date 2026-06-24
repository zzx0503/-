package com.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.config.AiAgentProperties;
import com.bookstore.config.AiProperties;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.CartItem;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.CartItemMapper;
import com.bookstore.mapper.FavoriteMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.bookstore.mapper.SearchHistoryMapper;
import com.bookstore.service.ai.AiAgentClient;
import com.bookstore.service.ai.AiClient;
import com.bookstore.service.ai.AiClient.ChatMsg;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBookAgentService {

    private final AiClient aiClient;
    private final AiAgentClient aiAgentClient;
    private final AiProperties aiProps;
    private final AiAgentProperties agentProps;
    private final BookMapper bookMapper;
    private final CartItemMapper cartItemMapper;
    private final FavoriteMapper favoriteMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final OssUrlBuilder ossUrlBuilder;
    private final UserReadingProfileService readingProfileService;

    private static final int CANDIDATE_LIMIT = 30;

    // Self-injection for @Cacheable proxy (Lazy avoids circular dependency)
    @Autowired
    @Lazy
    private AiBookAgentService self;

    public List<BookListVO> aiSearch(String keyword, Long userId) {
        long t0 = System.currentTimeMillis();
        if (!hasText(keyword)) {
            return Collections.emptyList();
        }
        String kw = keyword.trim();
        log.info("[aiSearch] query=\"{}\" userId={}", kw, userId);

        // Fast path: direct DB search for specific titles/authors (short, non-conversational queries)
        if (isDirectLookup(kw)) {
            List<Book> direct = bookMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                    .and(w -> w.like(Book::getTitle, kw).or().like(Book::getAuthor, kw))
                    .orderByDesc(Book::getSalesCount).last("LIMIT 20")
            );
            if (!direct.isEmpty()) {
                log.info("[aiSearch] 快速通道 {}ms, results={}", System.currentTimeMillis() - t0, direct.size());
                return direct.stream().map(this::toListVO).collect(Collectors.toList());
            }
        }

        // Agent path: conversational/semantic queries
        // 优先使用缓存的 LLM 用户画像分析，没有则用原始数据
        boolean profileAnalyzed = false;
        String userProfile = null;
        if (userId != null) {
            String cached = readingProfileService.getProfileAnalysis(userId);
            if (cached != null) {
                userProfile = cached;
                profileAnalyzed = true;
                log.info("[aiSearch] 使用缓存画像 userId={}", userId);
            } else {
                userProfile = buildRawUserProfile(userId);
                log.info("[aiSearch] 无缓存画像，使用原始数据 userId={}", userId);
            }
        }

        List<Book> candidates = self.getHotCandidates();
        List<Map<String, Object>> candidateMaps = candidates.stream()
            .map(this::toBookMap).toList();

        if (agentProps.isEnabled()) {
            try {
                var cacheValue = self.getCachedAgentResults(kw, userId, candidateMaps, userProfile, profileAnalyzed);
                if (cacheValue != null && cacheValue.getBookIds() != null && !cacheValue.getBookIds().isEmpty()) {
                    List<BookListVO> result = fetchBooksByIds(cacheValue.getBookIds());
                    log.info("[aiSearch] Agent {}ms, {} books", System.currentTimeMillis() - t0, result.size());

                    // 当使用原始数据且 Agent 返回了分析结果时，同步写入缓存表供下次使用
                    if (!profileAnalyzed && hasText(cacheValue.getAnalysis()) && userId != null) {
                        readingProfileService.saveAnalysis(userId, cacheValue.getAnalysis());
                    }

                    if (hasText(cacheValue.getAnalysis())) {
                        log.info("[aiSearch] 用户画像分析 ↓\n{}", cacheValue.getAnalysis());
                    }
                    if (cacheValue.getReasons() != null && !cacheValue.getReasons().isEmpty()) {
                        log.info("[aiSearch] 推荐原因: {}", cacheValue.getReasons());
                    }
                    return result;
                }
            } catch (Exception e) {
                if (!agentProps.isFallbackOnFailure()) throw e;
                log.warn("[aiSearch] Agent失败降级, error={}", e.getMessage());
            }
        }

        log.info("[aiSearch] Agent不可用, Total {}ms", System.currentTimeMillis() - t0);
        return Collections.emptyList();
    }

    private List<String> extractKeywords(String query) {
        try {
            List<ChatMsg> messages = List.of(
                ChatMsg.system(aiProps.getKeywordExtractionPrompt()),
                ChatMsg.user(query)
            );
            String reply = aiClient.chatCompletion(
                messages,
                aiProps.getSearchModel(),
                aiProps.getSearchApiBase(),
                aiProps.getSearchApiKey()
            );
            String cleaned = reply.replaceAll("[“\"'\"《》\\n]", "");
            String[] parts = cleaned.split("[,，、\\s]+");
            List<String> result = new ArrayList<>();
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty() && t.length() < 50) {
                    result.add(t);
                }
            }
            return result.isEmpty() ? List.of(query) : result;
        } catch (Exception e) {
            log.warn("Keyword extraction failed, using original query", e);
            return List.of(query);
        }
    }

    public List<BookListVO> aiRecommend(Long userId, Integer limit) {
        long t0 = System.currentTimeMillis();
        if (limit == null || limit < 1) limit = 10;

        String userProfile = buildRawUserProfile(userId);
        log.info("[aiRecommend] userId={}, profile=\"{}\"", userId, truncate(userProfile, 200));

        if (agentProps.isEnabled()) {
            try {
                var agentResp = aiAgentClient.recommend(userId, userProfile, Collections.emptyList(), limit);
                List<Long> ids = agentResp.bookIds();
                List<String> reasons = agentResp.reasons();
                log.info("[aiRecommend] Agent {}ms, {} books, {} reasons",
                    System.currentTimeMillis() - t0, ids.size(), reasons != null ? reasons.size() : 0);
                if (reasons != null && !reasons.isEmpty()) {
                    log.info("[aiRecommend] 推荐原因: {}", reasons);
                }
                if (!ids.isEmpty()) {
                    return fetchBooksByIds(ids);
                }
                log.warn("[aiRecommend] Agent返回空结果");
            } catch (Exception e) {
                if (!agentProps.isFallbackOnFailure()) throw e;
                log.warn("[aiRecommend] Agent失败降级, error={}", e.getMessage());
            }
        }

        log.info("[aiRecommend] 无结果, Total {}ms", System.currentTimeMillis() - t0);
        return Collections.emptyList();
    }

    // 中文停用词：去掉这些词后剩下的就是有检索意义的关键词
    private static final Set<String> STOP_WORDS = Set.of(
        "帮我", "推荐", "一本", "一下", "有没有", "什么", "哪些", "哪个", "我想",
        "可以", "能", "适合", "好看", "值得", "求", "找", "要", "想", "看", "买",
        "我", "你", "他", "她", "它", "我们", "你们", "他们",
        "的", "了", "在", "是", "有", "和", "与", "或", "等", "及",
        "不", "很", "都", "也", "就", "还", "把", "被", "让", "给",
        "吗", "吧", "呢", "啊", "哦", "嗯", "么", "嘛",
        "这", "那", "些", "个", "本", "册", "套",
        "比较", "非常", "特别", "挺", "更", "最",
        "关于", "对于", "根据", "按照",
        "书", "图书", "书籍"  // 太泛，在所有书中匹配没意义
    );

    private List<String> extractLocalKeywords(String query) {
        // 按停用词切分，保留有意义的片段
        String remaining = query;
        // 按长度从长到短排序，优先匹配长的停用词（如"有没有"优先于"有"）
        String[] sortedStopWords = STOP_WORDS.stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .toArray(String[]::new);
        for (String sw : sortedStopWords) {
            remaining = remaining.replace(sw, " ");
        }
        List<String> result = new ArrayList<>();
        for (String part : remaining.split("[\\s,，。！？、；：''【】《》（）\\p{Punct}]+")) {
            String t = part.trim();
            // 保留长度 >= 2 且有实际意义的词
            if (t.length() >= 2 && !t.matches("^[0-9\\p{Punct}]+$")) {
                result.add(t);
            }
        }
        return result.isEmpty() ? List.of(query) : result;
    }

    private List<Book> retrieveCandidates(String query, int limit) {
        List<String> keywords = extractLocalKeywords(query);
        log.info("[aiSearch] local keywords: {} from query: \"{}\"", keywords, query);

        Set<Long> ids = new LinkedHashSet<>();
        List<Book> result = new ArrayList<>();

        for (String kw : keywords) {
            if (result.size() >= limit) break;

            // 1. Title / author LIKE match
            int remaining = limit - result.size();
            List<Book> titleMatch = bookMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                    .and(w -> w.like(Book::getTitle, kw).or().like(Book::getAuthor, kw))
                    .notIn(!ids.isEmpty(), Book::getId, ids)
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + remaining)
            );
            for (Book b : titleMatch) {
                if (ids.add(b.getId())) result.add(b);
            }

            // 2. Description LIKE match
            if (result.size() < limit) {
                remaining = limit - result.size();
                List<Book> descMatch = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                        .like(Book::getDescription, kw)
                        .notIn(!ids.isEmpty(), Book::getId, ids)
                        .orderByDesc(Book::getSalesCount)
                        .last("LIMIT " + remaining)
                );
                for (Book b : descMatch) {
                    if (ids.add(b.getId())) result.add(b);
                }
            }

            // 3. Category name LIKE match
            if (result.size() < limit) {
                remaining = limit - result.size();
                List<Book> catMatch = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                        .apply("category_id IN (SELECT id FROM category WHERE name LIKE {0} AND deleted = 0)",
                            "%" + kw + "%")
                        .notIn(!ids.isEmpty(), Book::getId, ids)
                        .orderByDesc(Book::getSalesCount)
                        .last("LIMIT " + remaining)
                );
                for (Book b : catMatch) {
                    if (ids.add(b.getId())) result.add(b);
                }
            }
        }

        // 4. Hot sellers fallback
        if (result.size() < limit) {
            int remaining = limit - result.size();
            List<Book> hot = bookMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                    .notIn(!ids.isEmpty(), Book::getId, ids)
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + remaining)
            );
            for (Book b : hot) {
                if (ids.add(b.getId())) result.add(b);
            }
        }
        return result;
    }

    private List<Book> retrieveCandidates(List<String> keywords, int limit) {
        Set<Long> ids = new LinkedHashSet<>();
        List<Book> result = new ArrayList<>();

        for (String kw : keywords) {
            if (result.size() >= limit) break;

            // 1. Title / author match
            int remaining = limit - result.size();
            List<Book> titleMatch = bookMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                    .and(w -> w.like(Book::getTitle, kw).or().like(Book::getAuthor, kw))
                    .notIn(!ids.isEmpty(), Book::getId, ids)
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + remaining)
            );
            for (Book b : titleMatch) {
                if (ids.add(b.getId())) result.add(b);
            }

            // 2. Description match
            if (result.size() < limit) {
                remaining = limit - result.size();
                List<Book> descMatch = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                        .like(Book::getDescription, kw)
                        .notIn(!ids.isEmpty(), Book::getId, ids)
                        .orderByDesc(Book::getSalesCount)
                        .last("LIMIT " + remaining)
                );
                for (Book b : descMatch) {
                    if (ids.add(b.getId())) result.add(b);
                }
            }

            // 3. Category name match
            if (result.size() < limit) {
                remaining = limit - result.size();
                List<Book> catMatch = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                        .apply("category_id IN (SELECT id FROM category WHERE name LIKE {0} AND deleted = 0)",
                            "%" + kw + "%")
                        .notIn(!ids.isEmpty(), Book::getId, ids)
                        .orderByDesc(Book::getSalesCount)
                        .last("LIMIT " + remaining)
                );
                for (Book b : catMatch) {
                    if (ids.add(b.getId())) result.add(b);
                }
            }
        }

        // 4. Hot sellers fallback
        if (result.size() < limit) {
            int remaining = limit - result.size();
            List<Book> hot = bookMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                    .notIn(!ids.isEmpty(), Book::getId, ids)
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + remaining)
            );
            for (Book b : hot) {
                if (ids.add(b.getId())) result.add(b);
            }
        }
        return result;
    }

    private List<Book> retrieveRecommendCandidates(Set<Long> excludeIds, int limit) {
        Set<Long> ids = new LinkedHashSet<>();
        List<Book> result = new ArrayList<>();

        // 高评分 + 热销
        List<Book> books = bookMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .notIn(!excludeIds.isEmpty(), Book::getId, excludeIds)
                .orderByDesc(Book::getRating)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + limit)
        );
        for (Book b : books) {
            if (ids.add(b.getId())) result.add(b);
        }
        return result;
    }

    // ========== 缓存相关方法 ==========

    @Cacheable(value = "ai:search:candidates", key = "'hot30'")
    public List<Book> getHotCandidates() {
        return bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1).eq(Book::getDeleted, 0)
                .orderByDesc(Book::getSalesCount).last("LIMIT 30")
        );
    }

    @Cacheable(value = "ai:search:results:v2", key = "#keyword + '::' + (#userId != null ? #userId : 'anon')", unless = "#result == null || #result.bookIds == null || #result.bookIds.isEmpty()")
    public SearchCacheValue getCachedAgentResults(String keyword, Long userId,
                                                  List<Map<String, Object>> candidateMaps,
                                                  String userProfile, boolean profileAnalyzed) {
        var agentResp = aiAgentClient.search(keyword, candidateMaps, userId, userProfile, profileAnalyzed);
        return new SearchCacheValue(agentResp.bookIds(), agentResp.reasons(), agentResp.analysis());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchCacheValue {
        private List<Long> bookIds;
        private List<String> reasons;
        private String analysis;
    }

    private String buildCandidatePrompt(List<Book> books) {
        StringBuilder sb = new StringBuilder("候选图书列表:\n");
        int idx = 1;
        for (Book b : books) {
            sb.append(idx++).append(". ID:").append(b.getId())
              .append(" 《").append(safe(b.getTitle())).append("》");
            if (hasText(b.getAuthor())) sb.append(" 作者:").append(b.getAuthor());
            if (b.getPrice() != null) sb.append(" 价格:¥").append(b.getPrice().toPlainString());
            if (hasText(b.getDescription())) {
                String desc = b.getDescription().replaceAll("\\s+", " ");
                sb.append(" 简介:").append(desc.length() > 200 ? desc.substring(0, 200) + "..." : desc);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 构建原始用户数据文本（直接从 DB 拉取），供 LLM 分析或无缓存时使用
     */
    private String buildRawUserProfile(Long userId) {
        StringBuilder sb = new StringBuilder();
        try {
            // 收藏偏好
            List<com.bookstore.domain.po.Favorite> favorites = favoriteMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.Favorite>()
                    .eq(com.bookstore.domain.po.Favorite::getUserId, userId)
                    .eq(com.bookstore.domain.po.Favorite::getDeleted, 0)
                    .orderByDesc(com.bookstore.domain.po.Favorite::getCreateTime)
                    .last("LIMIT 5")
            );
            if (!favorites.isEmpty()) {
                sb.append("收藏的图书:");
                for (com.bookstore.domain.po.Favorite f : favorites) {
                    Book b = bookMapper.selectById(f.getBookId());
                    if (b != null) sb.append("《").append(b.getTitle()).append("》");
                }
                sb.append("; ");
            }

            // 购物车中的图书
            List<CartItem> cartItems = cartItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getDeleted, 0)
                    .orderByDesc(CartItem::getCreateTime)
                    .last("LIMIT 5")
            );
            if (!cartItems.isEmpty()) {
                sb.append("购物车中的图书:");
                for (CartItem ci : cartItems) {
                    Book b = bookMapper.selectById(ci.getBookId());
                    if (b != null) sb.append("《").append(b.getTitle()).append("》");
                }
                sb.append("; ");
            }

            // 订单中购买过的图书
            List<com.bookstore.domain.po.OrderMain> orders = orderMainMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.OrderMain>()
                    .eq(com.bookstore.domain.po.OrderMain::getUserId, userId)
                    .ne(com.bookstore.domain.po.OrderMain::getStatus, "CANCELLED")
                    .eq(com.bookstore.domain.po.OrderMain::getDeleted, 0)
                    .orderByDesc(com.bookstore.domain.po.OrderMain::getCreateTime)
                    .last("LIMIT 5")
            );
            if (!orders.isEmpty()) {
                sb.append("购买过的图书:");
                for (com.bookstore.domain.po.OrderMain o : orders) {
                    List<com.bookstore.domain.po.OrderItem> items = orderItemMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.OrderItem>()
                            .eq(com.bookstore.domain.po.OrderItem::getOrderId, o.getId())
                    );
                    for (com.bookstore.domain.po.OrderItem oi : items) {
                        Book b = bookMapper.selectById(oi.getBookId());
                        if (b != null) sb.append("《").append(b.getTitle()).append("》");
                    }
                }
                sb.append("; ");
            }

            // 搜索历史
            List<com.bookstore.domain.po.SearchHistory> histories = searchHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.SearchHistory>()
                    .eq(com.bookstore.domain.po.SearchHistory::getUserId, userId)
                    .eq(com.bookstore.domain.po.SearchHistory::getDeleted, 0)
                    .orderByDesc(com.bookstore.domain.po.SearchHistory::getCreateTime)
                    .last("LIMIT 5")
            );
            if (!histories.isEmpty()) {
                sb.append("搜索过:");
                for (com.bookstore.domain.po.SearchHistory h : histories) {
                    sb.append(h.getKeyword()).append(" ");
                }
            }
        } catch (Exception e) {
            log.warn("buildUserProfile failed", e);
        }
        return sb.toString().trim();
    }

    private Set<Long> getUserInteractedBookIds(Long userId) {
        Set<Long> ids = new java.util.HashSet<>();
        try {
            List<com.bookstore.domain.po.Favorite> favorites = favoriteMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.Favorite>()
                    .eq(com.bookstore.domain.po.Favorite::getUserId, userId)
                    .eq(com.bookstore.domain.po.Favorite::getDeleted, 0)
            );
            favorites.forEach(f -> ids.add(f.getBookId()));

            List<com.bookstore.domain.po.OrderMain> orders = orderMainMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.OrderMain>()
                    .eq(com.bookstore.domain.po.OrderMain::getUserId, userId)
                    .ne(com.bookstore.domain.po.OrderMain::getStatus, "PENDING_PAY")
                    .ne(com.bookstore.domain.po.OrderMain::getStatus, "CANCELLED")
                    .eq(com.bookstore.domain.po.OrderMain::getDeleted, 0)
            );
            for (com.bookstore.domain.po.OrderMain order : orders) {
                List<com.bookstore.domain.po.OrderItem> items = orderItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.OrderItem>()
                        .eq(com.bookstore.domain.po.OrderItem::getOrderId, order.getId())
                );
                items.forEach(i -> ids.add(i.getBookId()));
            }
        } catch (Exception e) {
            log.warn("getUserInteractedBookIds failed", e);
        }
        return ids;
    }

    private List<Long> parseIdList(String reply) {
        if (!hasText(reply)) return Collections.emptyList();
        String cleaned = reply.trim().toLowerCase();
        if (cleaned.contains("empty") || cleaned.contains("无") || cleaned.contains("没有")) {
            return Collections.emptyList();
        }
        cleaned = cleaned.replaceAll("[^0-9,]", "");
        List<Long> ids = new ArrayList<>();
        for (String part : cleaned.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(t));
            } catch (NumberFormatException ignore) {
            }
        }
        return ids;
    }

    private List<BookListVO> fetchBooksByIds(List<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        List<Book> books = bookMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                .in(Book::getId, ids)
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
        );
        java.util.Map<Long, Book> map = books.stream()
            .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a, java.util.LinkedHashMap::new));
        List<BookListVO> result = new ArrayList<>();
        for (Long id : ids) {
            Book b = map.get(id);
            if (b != null) result.add(toListVO(b));
        }
        return result;
    }

    private BookListVO toListVO(Book b) {
        BookListVO vo = new BookListVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setAuthor(b.getAuthor());
        vo.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
        vo.setPrice(b.getPrice());
        vo.setOriginalPrice(b.getOriginalPrice());
        vo.setSalesCount(b.getSalesCount());
        vo.setRating(b.getRating());
        vo.setCategoryId(b.getCategoryId());
        return vo;
    }

    private Map<String, Object> toBookMap(Book b) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("title", b.getTitle());
        m.put("author", b.getAuthor());
        m.put("price", b.getPrice() == null ? null : b.getPrice().toPlainString());
        m.put("description", b.getDescription());
        m.put("rating", b.getRating());
        m.put("salesCount", b.getSalesCount());
        return m;
    }

    // 判断是否为直接书名/作者查找（短词、非自然语言）
    private static final Set<String> CONVERSATIONAL = Set.of(
        "推荐", "帮我", "有没有", "什么", "哪些", "哪个", "我想", "想要",
        "适合", "好看", "值得", "求", "找", "可以", "给我", "一本",
        "一下", "看看", "下雨", "周末", "睡前", "过年", "放假"
    );

    private static boolean isDirectLookup(String query) {
        if (query.length() > 8) return false; // 长文本大概率是自然语言
        for (String cw : CONVERSATIONAL) {
            if (query.contains(cw)) return false;
        }
        return true;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
