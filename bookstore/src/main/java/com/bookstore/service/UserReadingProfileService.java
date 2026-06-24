package com.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.config.AiProperties;
import com.bookstore.domain.po.*;
import com.bookstore.mapper.*;
import com.bookstore.service.ai.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReadingProfileService {

    private final UserReadingProfileMapper profileMapper;
    private final FavoriteMapper favoriteMapper;
    private final CartItemMapper cartItemMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final BookMapper bookMapper;
    private final AiClient aiClient;
    private final AiProperties aiProps;

    // 自注入代理，使 @Transactional 生效
    @Autowired
    @Lazy
    private UserReadingProfileService self;

    private static final int PROFILE_DAYS = 7;

    /**
     * 获取缓存的用户画像分析结果。
     * 如果缓存过期了也返回（总比没有好），后台定时任务会异步刷新。
     */
    public String getProfileAnalysis(Long userId) {
        if (userId == null) return null;
        try {
            UserReadingProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserReadingProfile>()
                    .eq(UserReadingProfile::getUserId, userId)
                    .eq(UserReadingProfile::getDeleted, 0)
            );
            if (profile != null && profile.getProfileAnalysis() != null) {
                return profile.getProfileAnalysis(); // 过期也返回，后台刷新
            }
        } catch (Exception e) {
            log.warn("[用户画像] 读取缓存失败 userId={}", userId, e);
        }
        return null;
    }

    /**
     * 保存用户画像分析结果（由搜索流程在 Agent 返回分析时调用）。
     * 用于新用户首次搜索时实时写入缓存。
     */
    public void saveAnalysis(Long userId, String analysis) {
        if (userId == null || !hasText(analysis)) return;
        try {
            UserReadingProfile existing = profileMapper.selectOne(
                new LambdaQueryWrapper<UserReadingProfile>()
                    .eq(UserReadingProfile::getUserId, userId)
            );
            LocalDateTime expireTime = LocalDateTime.now().plusDays(PROFILE_DAYS);
            if (existing != null) {
                existing.setProfileAnalysis(analysis);
                existing.setExpireTime(expireTime);
                profileMapper.updateById(existing);
            } else {
                UserReadingProfile profile = new UserReadingProfile();
                profile.setUserId(userId);
                profile.setProfileAnalysis(analysis);
                profile.setExpireTime(expireTime);
                profile.setRefreshCount(0);
                profile.setLastRefreshStatus("ONDEMAND");
                profileMapper.insert(profile);
            }
            log.debug("[用户画像] 实时写入成功 userId={}", userId);
        } catch (Exception e) {
            log.warn("[用户画像] 实时写入失败 userId={}", userId, e);
        }
    }

    // ========== 定时刷新任务 ==========

    /**
     * 每 2 小时执行一次，刷新所有已过期的用户画像。
     * 白天（8:00-22:00）慢速处理避免服务器压力，夜间加速。
     */
    @Scheduled(cron = "0 30 */2 * * ?")
    public void refreshExpiredProfiles() {
        if (!aiProps.isEnabled()) {
            log.debug("[用户画像] AI 未启用，跳过刷新");
            return;
        }

        int hour = LocalDateTime.now().getHour();
        boolean isDaytime = hour >= 8 && hour < 22;
        int batchLimit = isDaytime ? 20 : 60;       // 白天少处理，夜间多处理
        int delayMs = isDaytime ? 1000 : 300;        // 白天间隔 1s，夜间 300ms

        List<UserReadingProfile> expired = profileMapper.selectList(
            new LambdaQueryWrapper<UserReadingProfile>()
                .eq(UserReadingProfile::getDeleted, 0)
                .lt(UserReadingProfile::getExpireTime, LocalDateTime.now())
                .last("LIMIT " + batchLimit)
        );

        log.info("[用户画像] 开始刷新过期画像 (hour={}, limit={}, delay={}ms, count={})", hour, batchLimit, delayMs, expired.size());

        int success = 0, fail = 0;
        for (UserReadingProfile profile : expired) {
            try {
                self.refreshProfile(profile.getUserId());
                success++;
            } catch (Exception e) {
                log.warn("[用户画像] 刷新失败 userId={}", profile.getUserId(), e);
                fail++;
            }
            if (expired.size() > 1) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
            }
        }

        log.info("[用户画像] 刷新过期完成, 成功={}, 失败={}", success, fail);
    }

    // ========== 单用户刷新 ==========

    /**
     * 对指定用户重新构建阅读画像。
     * 流程：拉取原始数据 → 调用 LLM 分析 → 写入缓存表
     */
    @Transactional
    public void refreshProfile(Long userId) {
        String rawData = buildRawProfileData(userId);
        if (rawData.isEmpty()) {
            log.warn("[用户画像] 用户 {} 无任何数据，跳过", userId);
            return;
        }

        String analysis = analyzeWithLLM(rawData);

        UserReadingProfile existing = profileMapper.selectOne(
            new LambdaQueryWrapper<UserReadingProfile>()
                .eq(UserReadingProfile::getUserId, userId)
        );

        LocalDateTime expireTime = LocalDateTime.now().plusDays(PROFILE_DAYS);

        if (existing != null) {
            existing.setProfileAnalysis(analysis);
            existing.setProfileDataSnapshot(rawData);
            existing.setExpireTime(expireTime);
            existing.setRefreshCount(existing.getRefreshCount() == null ? 1 : existing.getRefreshCount() + 1);
            existing.setLastRefreshStatus("SUCCESS");
            profileMapper.updateById(existing);
        } else {
            UserReadingProfile profile = new UserReadingProfile();
            profile.setUserId(userId);
            profile.setProfileAnalysis(analysis);
            profile.setProfileDataSnapshot(rawData);
            profile.setExpireTime(expireTime);
            profile.setRefreshCount(1);
            profile.setLastRefreshStatus("SUCCESS");
            profileMapper.insert(profile);
        }

        log.info("[用户画像] 刷新完成 userId={}, analysis=\"{}\"", userId, truncate(analysis, 100));
    }

    // ========== 私有方法 ==========

    /**
     * 拉取用户原始数据（收藏 + 购物车 + 订单 + 搜索历史）
     */
    private String buildRawProfileData(Long userId) {
        StringBuilder sb = new StringBuilder();

        // 收藏
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDeleted, 0)
                .orderByDesc(Favorite::getCreateTime)
                .last("LIMIT 10")
        );
        if (!favorites.isEmpty()) {
            sb.append("收藏的图书:");
            for (Favorite f : favorites) {
                Book b = bookMapper.selectById(f.getBookId());
                if (b != null) sb.append("《").append(b.getTitle()).append("》");
            }
            sb.append("; ");
        }

        // 购物车
        List<CartItem> cartItems = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDeleted, 0)
                .orderByDesc(CartItem::getCreateTime)
                .last("LIMIT 10")
        );
        if (!cartItems.isEmpty()) {
            sb.append("购物车中的图书:");
            for (CartItem ci : cartItems) {
                Book b = bookMapper.selectById(ci.getBookId());
                if (b != null) sb.append("《").append(b.getTitle()).append("》");
            }
            sb.append("; ");
        }

        // 订单
        List<OrderMain> orders = orderMainMapper.selectList(
            new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getUserId, userId)
                .ne(OrderMain::getStatus, "CANCELLED")
                .eq(OrderMain::getDeleted, 0)
                .orderByDesc(OrderMain::getCreateTime)
                .last("LIMIT 5")
        );
        if (!orders.isEmpty()) {
            sb.append("购买过的图书:");
            for (OrderMain o : orders) {
                List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, o.getId())
                );
                for (OrderItem oi : items) {
                    Book b = bookMapper.selectById(oi.getBookId());
                    if (b != null) sb.append("《").append(b.getTitle()).append("》");
                }
            }
            sb.append("; ");
        }

        // 搜索历史
        List<SearchHistory> histories = searchHistoryMapper.selectList(
            new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId)
                .eq(SearchHistory::getDeleted, 0)
                .orderByDesc(SearchHistory::getCreateTime)
                .last("LIMIT 10")
        );
        if (!histories.isEmpty()) {
            sb.append("搜索过:");
            for (SearchHistory h : histories) {
                sb.append(h.getKeyword()).append(" ");
            }
        }

        return sb.toString().trim();
    }

    /**
     * 调用 LLM 分析用户阅读偏好
     */
    private String analyzeWithLLM(String rawData) {
        String prompt = """
你是一位用户画像分析专家。请根据用户的阅读数据，分析这个读者的偏好特点。

数据：
%s

请从以下角度分析（100 字以内，简洁）：
1. 偏好什么类型/题材的书籍？
2. 喜欢哪些作者或风格？
3. 这是一个什么样的读者群体？
""".formatted(rawData);

        try {
            String result = aiClient.chatCompletion(
                List.of(
                    AiClient.ChatMsg.system("你是一位用户画像分析专家，输出简洁精准的分析结果。"),
                    AiClient.ChatMsg.user(prompt)
                ),
                aiProps.getSearchModel()
            );
            return result.length() > 500 ? result.substring(0, 500) : result;
        } catch (Exception e) {
            log.warn("[用户画像] LLM 分析失败", e);
            // 降级：使用原始数据摘要作为分析
            return "用户阅读数据: " + (rawData.length() > 200 ? rawData.substring(0, 200) + "..." : rawData);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
