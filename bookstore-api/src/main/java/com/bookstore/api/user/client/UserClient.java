package com.bookstore.api.user.client;

import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.api.user.dto.UserProfileDTO;
import com.bookstore.api.user.fallback.UserClientFallbackFactory;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/api/internal/user/{id}")
    Result<UserProfileDTO> getUser(@PathVariable("id") Long id);

    @GetMapping("/api/internal/address/{id}")
    Result<AddressDTO> getAddress(@PathVariable("id") Long id);

    @PostMapping("/api/internal/wallet/pay")
    Result<Void> pay(@RequestParam("userId") Long userId,
                     @RequestParam("orderNo") String orderNo,
                     @RequestParam("amount") BigDecimal amount);
}
