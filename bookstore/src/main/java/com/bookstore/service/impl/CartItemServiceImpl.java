package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookstore.domain.dto.cart.CartItemFormDTO;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.CartItem;
import com.bookstore.domain.vo.cart.CartItemVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.CartItemMapper;
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
    private final BookMapper bookMapper;
    private final OssUrlBuilder ossUrlBuilder;

    @Override
    @Transactional
    public CartItemVO addToCart(Long userId, CartItemFormDTO dto) {
        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null || book.getDeleted() == 1 || book.getStatus() != 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        if (book.getStock() < dto.getQuantity()) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }

        // 先尝试查询是否存在
        CartItem exist = cartItemMapper.selectOne(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getBookId, dto.getBookId())
                .eq(CartItem::getDeleted, 0)
        );
        
        if (exist != null) {
            // 已存在，更新数量
            int newQty = exist.getQuantity() + dto.getQuantity();
            if (newQty > book.getStock()) {
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
            }
            exist.setQuantity(newQty);
            exist.setSelected(1);
            cartItemMapper.updateById(exist);
            return toVO(exist, book);
        }

        // 不存在，尝试插入
        try {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setBookId(dto.getBookId());
            item.setQuantity(dto.getQuantity());
            item.setSelected(1);
            cartItemMapper.insert(item);
            return toVO(item, book);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发或逻辑删除记录导致重复键，查询包括已删除的记录进行恢复
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
            Book b = bookMapper.selectById(i.getBookId());
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
        Book book = bookMapper.selectById(item.getBookId());
        if (book != null && quantity > book.getStock()) {
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

    private CartItemVO toVO(CartItem item, Book book) {
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
