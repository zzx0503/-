package com.bookstore.service;

import com.bookstore.domain.dto.seckill.SeckillBuyDTO;
import com.bookstore.domain.vo.seckill.SeckillBuyResultVO;
import com.bookstore.domain.vo.seckill.SeckillOrderVO;
import com.bookstore.response.PageResult;

public interface SeckillService {

    SeckillBuyResultVO buy(Long userId, SeckillBuyDTO dto);

    SeckillBuyResultVO createOrderFromQueue(Long userId, Long activityId, Long addressId);

    void payOrder(Long userId, String orderNo);

    void cancelOrder(Long userId, String orderNo);

    SeckillOrderVO detail(Long userId, String orderNo);

    PageResult<SeckillOrderVO> listMyOrders(Long userId, String status, Integer page, Integer size);

    int autoExpire();
}
