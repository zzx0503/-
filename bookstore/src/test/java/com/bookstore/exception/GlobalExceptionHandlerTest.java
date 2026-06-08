package com.bookstore.exception;

import com.bookstore.response.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void business_exception_returns_corresponding_code() throws Exception {
        mvc.perform(get("/test/biz"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(1001))
           .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    @Test
    void unknown_exception_returns_500() throws Exception {
        mvc.perform(get("/test/boom"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(500));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/biz")
        public void biz() { throw new BusinessException(ResultCode.USER_NOT_FOUND); }

        @GetMapping("/test/boom")
        public void boom() { throw new RuntimeException("unexpected"); }
    }
}
