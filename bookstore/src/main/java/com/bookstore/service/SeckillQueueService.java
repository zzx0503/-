package com.bookstore.service;

import com.bookstore.domain.dto.seckill.SeckillBuyDTO;
import com.bookstore.domain.vo.seckill.SeckillQueueStatusVO;

public interface SeckillQueueService {

    String enqueue(Long userId, SeckillBuyDTO dto);

    SeckillQueueStatusVO queryStatus(String requestId);

    void consume(String message);
}
