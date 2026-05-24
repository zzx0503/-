package com.bookstore.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void of_should_compute_total_pages() {
        PageResult<String> p = PageResult.of(List.of("a", "b"), 25L, 1, 10);
        assertThat(p.getList()).containsExactly("a", "b");
        assertThat(p.getTotal()).isEqualTo(25L);
        assertThat(p.getPageNum()).isEqualTo(1);
        assertThat(p.getPageSize()).isEqualTo(10);
        assertThat(p.getPages()).isEqualTo(3);
    }

    @Test
    void empty_total_yields_zero_pages() {
        PageResult<String> p = PageResult.of(List.of(), 0L, 1, 10);
        assertThat(p.getPages()).isEqualTo(0);
    }
}
