package com.bookstore.client;

import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/internal/address/{id}")
    Result<AddressVO> getAddress(@PathVariable("id") Long id);

    @PostMapping("/api/internal/wallet/pay")
    Result<Void> pay(@RequestParam("userId") Long userId,
                     @RequestParam("orderNo") String orderNo,
                     @RequestParam("amount") BigDecimal amount);
}
