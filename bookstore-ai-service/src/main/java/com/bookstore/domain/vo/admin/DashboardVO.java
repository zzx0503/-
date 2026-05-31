package com.bookstore.domain.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {

    private long totalUsers;
    private long newUsersToday;
    private long totalBooks;
    private long onSaleBooks;

    private long totalOrders;
    private long ordersToday;
    private long pendingPayOrders;
    private long paidOrders;
    private long shippedOrders;
    private long completedOrders;
    private long cancelledOrders;

    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;

    private long runningSeckillActivities;
    private long runningCouponTemplates;

    private List<TopBookItem> topBooks;
    private Map<String, Long> orderStatusBreakdown;
    private List<DailySalesItem> last7DaysSales;

    @Data
    public static class TopBookItem {
        private Long bookId;
        private String title;
        private String coverKey;
        private Integer salesCount;
        private BigDecimal price;
    }

    @Data
    public static class DailySalesItem {
        private String date;
        private long orderCount;
        private BigDecimal revenue;
    }
}
