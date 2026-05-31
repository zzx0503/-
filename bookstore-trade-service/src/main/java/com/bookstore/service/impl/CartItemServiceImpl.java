package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookstore.api.book.client.BookClient;
import com.bookstore.domain.dto.cart.CartItemFormDTO;
import com.bookstore.domain.po.CartItem;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.domain.vo.cart.CartItemVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CartItemMapper;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CartItemService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {

    private final CartItemMapper cartItemMapper;
    private final BookClient bookServiceClient;
    private final OssUrlBuilder ossUrlBuilder;

    private BookDetailDTO requireBook(Long bookId) {
        Result<BookDetailDTO> result = bookServiceClient.getBook(bookId);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        BookDetailDTO book = result.getData();
        if (book == null || book.getDeleted() == 1 || book.getStatus() != 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        return book;
    }

    @Override
    @Transactional
    public CartItemVO addToCart(Long userId, CartItemFormDTO dto) {
        BookDetailDTO book = requireBook(dto.getBookId());
        if (book.getStock() < dto.getQuantity()) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }

        CartItem exist = cartItemMapper.selectOne(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getBookId, dto.getBookId())
                .eq(CartItem::getDeleted, 0)
        );

        if (exist != null) {
            int newQty = exist.getQuantity() + dto.getQuantity();
            if (newQty > book.getStock()) {
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
            }
            exist.setQuantity(newQty);
            exist.setSelected(1);
            cartItemMapper.updateById(exist);
            return toVO(exist, book);
        }

        try {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setBookId(dto.getBookId());
            item.setQuantity(dto.getQuantity());
            item.setSelected(1);
            cartItemMapper.insert(item);
            return toVO(item, book);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            CartItem retryItem = cartItemMapper.selectOneIgnoreDeleted(userId, dto.getBookId());
            if (retryItem != null) {
                int newQty = retryItem.getQuantity() + dto.getQuantity();
                if (newQty > book.getStock()) {
                    throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
                }
                cartItemMapper.updateIgnoreDeleted(retryItem.getId(), newQty, 1);
                retryItem.setQuantity(newQty);
                retryItem.setSelected(1);
                retryItem.setDeleted(0);
                return toVO(retryItem, book);
            }
            throw e;
        }
    }

    @Override
    public List<CartItemVO> listCart(Long userId) {
        List<CartItem> rows = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDeleted, 0)
                .orderByDesc(CartItem::getCreateTime)
        );
        return rows.stream().map(i -> {
            BookDetailDTO b = requireBook(i.getBookId());
            return toVO(i, b);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartItemVO updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity < 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        CartItem item = requireOwn(userId, cartItemId);
        BookDetailDTO book = requireBook(item.getBookId());
        if (quantity > book.getStock()) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
        return toVO(item, book);
    }

    @Override
    @Transactional
    public void updateSelected(Long userId, Long cartItemId, Integer selected) {
        CartItem item = requireOwn(userId, cartItemId);
        item.setSelected(selected == 1 ? 1 : 0);
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long cartItemId) {
        requireOwn(userId, cartItemId);
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        cartItemMapper.delete(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
        );
    }

    private CartItem requireOwn(Long userId, Long cartItemId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return item;
    }

    private CartItemVO toVO(CartItem item, BookDetailDTO book) {
        CartItemVO vo = new CartItemVO();
        vo.setId(item.getId());
        vo.setBookId(item.getBookId());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setBookCover(ossUrlBuilder.toFullUrl(book.getCoverKey()));
            vo.setBookPrice(book.getPrice());
            vo.setBookStock(book.getStock());
        }
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }
}
