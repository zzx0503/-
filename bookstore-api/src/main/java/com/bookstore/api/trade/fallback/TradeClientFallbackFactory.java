package com.bookstore.api.trade.fallback;

import com.bookstore.api.trade.client.TradeClient;
import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeClientFallbackFactory implements FallbackFactory<TradeClient> {

    @Override
    public TradeClient create(Throwable cause) {
        log.error("调用 trade-service 失败", cause);
        return new TradeClient() {
            @Override
            public Result<OrderDetailDTO> getOrder(String orderNo) {
                return Result.fail(ResultCode.SERVER_ERROR, "交易服务暂不可用");
            }

            @Override
            public Result<OrderDetailDTO> getOrderById(Long id) {
                return Result.fail(ResultCode.SERVER_ERROR, "交易服务暂不可用");
            }
        };
    }
}
