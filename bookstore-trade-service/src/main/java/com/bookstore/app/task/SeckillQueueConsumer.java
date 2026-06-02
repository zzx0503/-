package com.bookstore.app.task;

import com.bookstore.config.mq.RabbitMQConfig;
import com.bookstore.service.SeckillQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillQueueConsumer {

    private final SeckillQueueService seckillQueueService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void consume(String message) {
        seckillQueueService.consume(message);
    }
}
