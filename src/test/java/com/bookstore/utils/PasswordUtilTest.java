package com.bookstore.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    void encode_then_matches() {
        String hash = PasswordUtil.encode("hello123");
        assertThat(PasswordUtil.matches("hello123", hash)).isTrue();
        assertThat(PasswordUtil.matches("HELLO123", hash)).isFalse();
    }

    @Test
    void each_encode_yields_different_hash() {
        String h1 = PasswordUtil.encode("same");
        String h2 = PasswordUtil.encode("same");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void matches_returns_false_for_null_inputs() {
        assertThat(PasswordUtil.matches(null, "x")).isFalse();
        assertThat(PasswordUtil.matches("x", null)).isFalse();
    }
}
