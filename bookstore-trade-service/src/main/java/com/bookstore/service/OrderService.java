package com.bookstore.service;

import com.bookstore.domain.dto.order.CreateOrderDTO;
import com.bookstore.domain.dto.order.PayOrderDTO;
import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.domain.vo.order.OrderVO;
import com.bookstore.response.PageResult;

public interface OrderService {

    OrderVO createOrder(Long userId, CreateOrderDTO dto);

    void payOrder(Long userId, String orderNo, PayOrderDTO dto);

    void cancelOrder(Long userId, String orderNo);

    void confirmReceive(Long userId, String orderNo);

    OrderDetailDTO detail(Long userId, String orderNo);

    PageResult<OrderVO> listMyOrders(Long userId, String status, Integer page, Integer size);

    void shipOrder(String orderNo);

    PageResult<OrderVO> listAllOrders(String status, Integer page, Integer size);
}
