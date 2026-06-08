package com.bookstore.service;

import com.bookstore.domain.dto.cart.CartItemFormDTO;
import com.bookstore.domain.vo.cart.CartItemVO;

import java.util.List;

public interface CartItemService {

    CartItemVO addToCart(Long userId, CartItemFormDTO dto);

    List<CartItemVO> listCart(Long userId);

    CartItemVO updateQuantity(Long userId, Long cartItemId, Integer quantity);

    void updateSelected(Long userId, Long cartItemId, Integer selected);

    void delete(Long userId, Long cartItemId);

    void clear(Long userId);
}
