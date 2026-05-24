package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.po.OrderMain;
import com.bookstore.domain.po.SeckillActivity;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.admin.DashboardVO;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.CouponTemplateMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.bookstore.mapper.SeckillActivityMapper;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.AdminStatsService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final OrderMainMapper orderMainMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final OssUrlBuilder ossUrlBuilder;

    @Override
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

        vo.setTotalUsers(zero(userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getDeleted, 0))));
        vo.setNewUsersToday(zero(userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .ge(User::getCreateTime, startOfToday)
                .lt(User::getCreateTime, endOfToday))));

        vo.setTotalBooks(zero(bookMapper.selectCount(
            new LambdaQueryWrapper<Book>().eq(Book::getDeleted, 0))));
        vo.setOnSaleBooks(zero(bookMapper.selectCount(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getDeleted, 0)
                .eq(Book::getStatus, 1))));

        List<OrderMain> allOrders = orderMainMapper.selectList(
            new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getDeleted, 0));
        vo.setTotalOrders(allOrders.size());
        long ordersToday = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal revenueToday = BigDecimal.ZERO;
        Map<String, Long> statusMap = new HashMap<>();
        for (OrderMain o : allOrders) {
            statusMap.merge(o.getStatus(), 1L, Long::sum);
            if (isPaidStatus(o.getStatus()) && o.getPayAmount() != null) {
                totalRevenue = totalRevenue.add(o.getPayAmount());
                if (o.getPayTime() != null
                    && !o.getPayTime().isBefore(startOfToday)
                    && o.getPayTime().isBefore(endOfToday)) {
                    revenueToday = revenueToday.add(o.getPayAmount());
                }
            }
            if (o.getCreateTime() != null
                && !o.getCreateTime().isBefore(startOfToday)
                && o.getCreateTime().isBefore(endOfToday)) {
                ordersToday++;
            }
        }
        vo.setOrdersToday(ordersToday);
        vo.setTotalRevenue(totalRevenue);
        vo.setRevenueToday(revenueToday);
        vo.setPendingPayOrders(statusMap.getOrDefault("PENDING_PAY", 0L));
        vo.setPaidOrders(statusMap.getOrDefault("PAID", 0L));
        vo.setShippedOrders(statusMap.getOrDefault("SHIPPED", 0L));
        vo.setCompletedOrders(statusMap.getOrDefault("COMPLETED", 0L));
        vo.setCancelledOrders(statusMap.getOrDefault("CANCELLED", 0L));
        vo.setOrderStatusBreakdown(statusMap);

        vo.setRunningSeckillActivities(zero(seckillActivityMapper.selectCount(
            new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getDeleted, 0)
                .eq(SeckillActivity::getStatus, "RUNNING"))));
        vo.setRunningCouponTemplates(zero(couponTemplateMapper.selectCount(
            new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getDeleted, 0)
                .eq(CouponTemplate::getStatus, "RUNNING"))));

        vo.setTopBooks(topBooks(8));
        vo.setLast7DaysSales(last7DaysSales(allOrders));
        return vo;
    }

    private List<DashboardVO.TopBookItem> topBooks(int limit) {
        List<Book> books = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getDeleted, 0)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + limit)
        );
        List<DashboardVO.TopBookItem> list = new ArrayList<>();
        for (Book b : books) {
            DashboardVO.TopBookItem it = new DashboardVO.TopBookItem();
            it.setBookId(b.getId());
            it.setTitle(b.getTitle());
            it.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
            it.setSalesCount(b.getSalesCount() == null ? 0 : b.getSalesCount());
            it.setPrice(b.getPrice());
            list.add(it);
        }
        return list;
    }

    private List<DashboardVO.DailySalesItem> last7DaysSales(List<OrderMain> orders) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, DashboardVO.DailySalesItem> bucket = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            String key = today.minusDays(i).format(fmt);
            DashboardVO.DailySalesItem item = new DashboardVO.DailySalesItem();
            item.setDate(key);
            item.setOrderCount(0);
            item.setRevenue(BigDecimal.ZERO);
            bucket.put(key, item);
        }
        for (OrderMain o : orders) {
            if (o.getPayTime() == null || !isPaidStatus(o.getStatus())) {
                continue;
            }
            String key = o.getPayTime().toLocalDate().format(fmt);
            DashboardVO.DailySalesItem item = bucket.get(key);
            if (item == null) continue;
            item.setOrderCount(item.getOrderCount() + 1);
            item.setRevenue(item.getRevenue().add(o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount()));
        }
        return new ArrayList<>(bucket.values());
    }

    private boolean isPaidStatus(String status) {
        return "PAID".equals(status) || "SHIPPED".equals(status) || "COMPLETED".equals(status);
    }

    private long zero(Long v) {
        return v == null ? 0L : v;
    }
}
