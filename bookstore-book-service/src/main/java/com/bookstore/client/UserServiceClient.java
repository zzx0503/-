package com.bookstore.client;

import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/internal/user/{id}")
    Result<UserProfileVO> getUser(@PathVariable("id") Long id);
}
