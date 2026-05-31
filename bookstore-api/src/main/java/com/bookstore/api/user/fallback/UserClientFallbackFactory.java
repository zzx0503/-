package com.bookstore.api.user.fallback;

import com.bookstore.api.user.client.UserClient;
import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.api.user.dto.UserProfileDTO;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("调用 user-service 失败", cause);
        return new UserClient() {
            @Override
            public Result<UserProfileDTO> getUser(Long id) {
                return Result.fail(ResultCode.SERVER_ERROR, "用户服务暂不可用");
            }

            @Override
            public Result<AddressDTO> getAddress(Long id) {
                return Result.fail(ResultCode.SERVER_ERROR, "用户服务暂不可用");
            }

            @Override
            public Result<Void> pay(Long userId, String orderNo, BigDecimal amount) {
                return Result.fail(ResultCode.SERVER_ERROR, "用户服务暂不可用");
            }
        };
    }
}
