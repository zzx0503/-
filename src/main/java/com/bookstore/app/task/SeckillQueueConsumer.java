package com.bookstore.app.task;

import com.bookstore.service.SeckillQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillQueueConsumer {

    private final SeckillQueueService seckillQueueService;

    @Scheduled(fixedDelay = 100)
    public void poll() {
        seckillQueueService.consume();
    }
}
