package com.bookstore.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextTest {

    @AfterEach
    void clean() { UserContext.clear(); }

    @Test
    void set_then_get_returns_same_user() {
        CurrentUser u = new CurrentUser(1L, "alice", "USER");
        UserContext.set(u);
        assertThat(UserContext.get().getUserId()).isEqualTo(1L);
        assertThat(UserContext.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void clear_drops_user() {
        UserContext.set(new CurrentUser(1L, "alice", "USER"));
        UserContext.clear();
        assertThat(UserContext.get()).isNull();
    }

    @Test
    void requireUserId_throws_when_no_user() {
        org.assertj.core.api.Assertions.assertThatThrownBy(UserContext::requireUserId)
            .isInstanceOf(IllegalStateException.class);
    }
}
