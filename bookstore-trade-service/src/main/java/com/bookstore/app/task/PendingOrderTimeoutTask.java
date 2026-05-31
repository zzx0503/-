package com.bookstore.app.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookstore.api.book.client.BookClient;
import com.bookstore.domain.po.OrderItem;
import com.bookstore.domain.po.OrderMain;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderTimeoutTask {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final BookClient bookServiceClient;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        List<OrderMain> orders = orderMainMapper.selectList(
            new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getStatus, "PENDING_PAY")
                .le(OrderMain::getCreateTime, deadline)
                .eq(OrderMain::getDeleted, 0)
        );
        for (OrderMain order : orders) {
            try {
                List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
                );
                for (OrderItem oi : items) {
                    bookServiceClient.restoreStock(oi.getBookId(), oi.getQuantity());
                }
                orderMainMapper.update(null,
                    new LambdaUpdateWrapper<OrderMain>()
                        .eq(OrderMain::getId, order.getId())
                        .set(OrderMain::getStatus, "CANCELLED")
                );
                log.info("Auto cancelled timeout order: {}", order.getOrderNo());
            } catch (Exception e) {
                log.error("Failed to cancel timeout order: {}", order.getOrderNo(), e);
            }
        }
    }
}
