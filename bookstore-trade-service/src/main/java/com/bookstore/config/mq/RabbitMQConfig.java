package com.bookstore.config.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 队列、交换机、绑定配置
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 秒杀队列 ====================
    public static final String SECKILL_QUEUE = "seckill.queue";
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    @Bean
    public Queue seckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }

    // ==================== 订单延时队列（DLX）====================
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.create";

    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    // 订单超时时间：30 分钟（毫秒）
    public static final long ORDER_TIMEOUT_TTL = 30 * 60 * 1000;

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", ORDER_TIMEOUT_TTL);
        args.put("x-dead-letter-exchange", ORDER_TIMEOUT_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    // 死信队列（实际消费）
    @Bean
    public Queue orderCancelQueue() {
        return new Queue(ORDER_CANCEL_QUEUE, true);
    }

    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(ORDER_TIMEOUT_EXCHANGE);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderTimeoutExchange()).with(ORDER_CANCEL_ROUTING_KEY);
    }
}
