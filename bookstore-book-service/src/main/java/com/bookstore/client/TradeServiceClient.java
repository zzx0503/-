package com.bookstore.client;

import com.bookstore.domain.vo.order.OrderDetailVO;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "trade-service")
public interface TradeServiceClient {

    @GetMapping("/api/internal/order/{orderNo}")
    Result<OrderDetailVO> getOrder(@PathVariable("orderNo") String orderNo);

    @GetMapping("/api/internal/order/by-id/{id}")
    Result<OrderDetailVO> getOrderById(@PathVariable("id") Long id);
}
