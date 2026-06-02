package com.bookstore.config.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookstore.api.book.client.BookClient;
import com.bookstore.domain.po.OrderItem;
import com.bookstore.domain.po.OrderMain;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutListener {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final BookClient bookServiceClient;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCEL_QUEUE)
    public void onOrderTimeout(String message) {
        try {
            TimeoutMessage timeoutMsg = fromJson(message, TimeoutMessage.class);
            if (timeoutMsg == null || timeoutMsg.orderId() == null) {
                log.warn("收到无效的订单超时消息: {}", message);
                return;
            }

            OrderMain order = orderMainMapper.selectById(timeoutMsg.orderId());
            if (order == null || !"PENDING_PAY".equals(order.getStatus())) {
                log.info("订单已处理或不存在, orderId={}", timeoutMsg.orderId());
                return;
            }

            // 回滚库存
            List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            for (OrderItem oi : items) {
                bookServiceClient.restoreStock(oi.getBookId(), oi.getQuantity());
            }

            // 取消订单
            orderMainMapper.update(null,
                new LambdaUpdateWrapper<OrderMain>()
                    .eq(OrderMain::getId, order.getId())
                    .set(OrderMain::getStatus, "CANCELLED")
            );

            log.info("MQ 自动取消超时订单: orderNo={}, orderId={}", order.getOrderNo(), order.getId());
        } catch (Exception e) {
            log.error("处理订单超时消息失败: {}", message, e);
        }
    }

    @SneakyThrows
    private <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    public record TimeoutMessage(Long orderId) {}
}
