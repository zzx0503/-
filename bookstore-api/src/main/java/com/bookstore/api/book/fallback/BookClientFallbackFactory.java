package com.bookstore.api.book.fallback;

import com.bookstore.api.book.client.BookClient;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookClientFallbackFactory implements FallbackFactory<BookClient> {

    @Override
    public BookClient create(Throwable cause) {
        log.error("调用 book-service 失败", cause);
        return new BookClient() {
            @Override
            public Result<BookDetailDTO> getBook(Long id) {
                return Result.fail(ResultCode.SERVER_ERROR, "商品服务暂不可用");
            }

            @Override
            public Result<Void> deductStock(Long id, Integer quantity) {
                return Result.fail(ResultCode.SERVER_ERROR, "商品服务暂不可用");
            }

            @Override
            public Result<Void> restoreStock(Long id, Integer quantity) {
                return Result.fail(ResultCode.SERVER_ERROR, "商品服务暂不可用");
            }
        };
    }
}
