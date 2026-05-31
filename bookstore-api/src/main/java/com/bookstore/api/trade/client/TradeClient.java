package com.bookstore.api.trade.client;

import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.api.trade.fallback.TradeClientFallbackFactory;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "trade-service", fallbackFactory = TradeClientFallbackFactory.class)
public interface TradeClient {

    @GetMapping("/api/internal/order/{orderNo}")
    Result<OrderDetailDTO> getOrder(@PathVariable("orderNo") String orderNo);

    @GetMapping("/api/internal/order/by-id/{id}")
    Result<OrderDetailDTO> getOrderById(@PathVariable("id") Long id);
}
