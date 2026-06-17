package com.bookstore.service;

import com.bookstore.domain.dto.cart.CartItemFormDTO;
import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.domain.vo.cart.CartItemVO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalToolService {

    private final BookService bookService;
    private final UserService userService;
    private final CartItemService cartItemService;

    public List<BookListVO> searchBooks(String keyword, int limit) {
        return bookService.search(keyword, 1, limit).getList();
    }

    public BookDetailVO getBookDetail(Long bookId) {
        return bookService.detail(bookId);
    }

    public UserProfileVO getUserProfile(Long userId) {
        try {
            return userService.getProfile(userId);
        } catch (BusinessException e) {
            log.warn("User profile not found for userId={}, returning empty", userId);
            return new UserProfileVO();
        }
    }

    public List<CartItemVO> listCart(Long userId) {
        return cartItemService.listCart(userId);
    }

    public CartItemVO addToCart(Long userId, Long bookId, int quantity) {
        var dto = new CartItemFormDTO();
        dto.setBookId(bookId);
        dto.setQuantity(quantity);
        return cartItemService.addToCart(userId, dto);
    }
}
