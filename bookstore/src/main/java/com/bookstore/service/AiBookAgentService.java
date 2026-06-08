package com.bookstore.service;

import com.bookstore.config.AiProperties;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.FavoriteMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.bookstore.mapper.SearchHistoryMapper;
import com.bookstore.service.ai.AiClient;
import com.bookstore.service.ai.AiClient.ChatMsg;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBookAgentService {

    private final AiClient aiClient;
    private final AiProperties aiProps;
    private final BookMapper bookMapper;
    private final FavoriteMapper favoriteMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final OssUrlBuilder ossUrlBuilder;

    private static final int CANDIDATE_LIMIT = 30;

    public List<BookListVO> aiSearch(String keyword) {
        if (!hasText(keyword)) {
            return Collections.emptyList();
        }
        String kw = keyword.trim();

        // Step 1: AI extracts searchable keywords from natural language
        List<String> keywords = extractKeywords(kw);
        log.debug("AI extracted keywords: {} from query: {}", keywords, kw);

        // Step 2: Use keywords to search database
        List<Book> candidates = retrieveCandidates(keywords, CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 3: AI ranks candidates by relevance
        try {
            String prompt = buildCandidatePrompt(candidates)
                + "\n用户搜索意图: \"" + kw + "\"\n"
                + "请从候选图书中选出最匹配的图书ID列表（纯数字逗号分隔，不要解释）：";

            List<ChatMsg> messages = List.of(
                ChatMsg.system(aiProps.getSearchSystemPrompt()),
                ChatMsg.user(prompt)
            );
            String reply = aiClient.chatCompletion(
                messages,
                aiProps.getSearchModel(),
                aiProps.getSearchApiBase(),
                aiProps.getSearchApiKey()
            );
            List<Long> ids = parseIdList(reply);
            return fetchBooksByIds(ids);
        } catch (Exception e) {
            log.warn("AI search failed, fallback to keyword match", e);
            return candidates.stream().map(this::toListVO).collect(Collectors.toList());
        }
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
        if (limit == null || limit < 1) limit = 10;

        Set<Long> excludeIds = getUserInteractedBookIds(userId);
        List<Book> candidates = retrieveRecommendCandidates(excludeIds, CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String userProfile = buildUserProfile(userId);
        try {
            String prompt = buildCandidatePrompt(candidates)
                + "\n用户画像: " + (hasText(userProfile) ? userProfile : "新用户，暂无偏好记录")
                + "\n请推荐 " + limit + " 本最适合该用户的图书ID（纯数字逗号分隔，不要解释）：";

            List<ChatMsg> messages = List.of(
                ChatMsg.system(aiProps.getRecommendSystemPrompt()),
                ChatMsg.user(prompt)
            );
            String reply = aiClient.chatCompletion(
                messages,
                aiProps.getRecommendModel(),
                aiProps.getRecommendApiBase(),
                aiProps.getRecommendApiKey()
            );
            List<Long> ids = parseIdList(reply);
            List<BookListVO> result = fetchBooksByIds(ids);
            if (result.size() < limit) {
                Set<Long> found = result.stream().map(BookListVO::getId).collect(Collectors.toSet());
                for (Book b : candidates) {
                    if (!found.contains(b.getId())) {
                        result.add(toListVO(b));
                        if (result.size() >= limit) break;
                    }
                }
            }
            return result.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("AI recommend failed, fallback to hot books", e);
            return candidates.stream().limit(limit).map(this::toListVO).collect(Collectors.toList());
        }
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
                sb.append(" 简介:").append(truncate(desc, 60));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String buildUserProfile(Long userId) {
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
                sb.append(" ");
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
