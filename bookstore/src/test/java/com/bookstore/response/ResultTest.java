package com.bookstore.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void success_should_return_200_and_data() {
        Result<String> r = Result.success("hello");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMsg()).isEqualTo("OK");
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    void success_without_data_should_return_null_data() {
        Result<Void> r = Result.success();
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isNull();
    }

    @Test
    void fail_should_return_given_code_and_msg() {
        Result<Void> r = Result.fail(ResultCode.PARAM_INVALID, "name is required");
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).isEqualTo("name is required");
        assertThat(r.getData()).isNull();
    }

    @Test
    void fail_with_default_msg_should_use_code_default_msg() {
        Result<Void> r = Result.fail(ResultCode.UNAUTHORIZED);
        assertThat(r.getCode()).isEqualTo(401);
        assertThat(r.getMsg()).isEqualTo("未登录或登录已过期");
    }
}
