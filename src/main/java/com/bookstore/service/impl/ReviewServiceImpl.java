package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.dto.review.ReviewFormDTO;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.OrderItem;
import com.bookstore.domain.po.OrderMain;
import com.bookstore.domain.po.Review;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.review.ReviewVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.bookstore.mapper.ReviewMapper;
import com.bookstore.mapper.UserMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.ResultCode;
import com.bookstore.service.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReviewVO createReview(Long userId, ReviewFormDTO dto) {
        OrderMain order = orderMainMapper.selectById(dto.getOrderId());
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "订单未完成，无法评价");
        }

        long itemCount = orderItemMapper.selectCount(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, dto.getOrderId())
                .eq(OrderItem::getBookId, dto.getBookId())
        );
        if (itemCount == 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "该订单未包含此图书");
        }

        Review exist = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getBookId, dto.getBookId())
                .eq(Review::getOrderId, dto.getOrderId())
                .eq(Review::getDeleted, 0)
        );
        if (exist != null) {
            throw new BusinessException(ResultCode.REVIEW_ALREADY_EXISTS);
        }

        if (dto.getImages() != null) {
            for (String key : dto.getImages()) {
                if (!key.startsWith("reviews/" + userId + "/")) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "非法图片路径");
                }
            }
        }

        Review r = new Review();
        r.setUserId(userId);
        r.setBookId(dto.getBookId());
        r.setOrderId(dto.getOrderId());
        r.setRating(dto.getRating());
        r.setContent(dto.getContent());
        r.setImages(toJson(dto.getImages()));
        reviewMapper.insert(r);

        updateBookRating(dto.getBookId());
        return toVO(r);
    }

    @Override
    public PageResult<ReviewVO> listByBook(Long bookId, Integer page, Integer size) {
        Page<Review> p = reviewMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Review>()
                .eq(Review::getBookId, bookId)
                .eq(Review::getDeleted, 0)
                .orderByDesc(Review::getCreateTime)
        );
        List<ReviewVO> vos = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review r = reviewMapper.selectById(reviewId);
        if (r == null || r.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        if (!r.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        reviewMapper.deleteById(reviewId);
        updateBookRating(r.getBookId());
    }

    private void updateBookRating(Long bookId) {
        List<Review> reviews = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getBookId, bookId)
                .eq(Review::getDeleted, 0)
        );
        BigDecimal avg = reviews.isEmpty()
            ? new BigDecimal("5.0")
            : reviews.stream()
                .map(Review::getRating)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);

        Book book = bookMapper.selectById(bookId);
        if (book != null) {
            book.setRating(avg);
            bookMapper.updateById(book);
        }
    }

    @SneakyThrows
    private String toJson(List<String> images) {
        if (images == null) return null;
        return objectMapper.writeValueAsString(images);
    }

    @SneakyThrows
    private List<String> fromJson(String json) {
        if (json == null) return Collections.emptyList();
        return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    }

    private ReviewVO toVO(Review r) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setBookId(r.getBookId());
        vo.setOrderId(r.getOrderId());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setImages(fromJson(r.getImages()));
        vo.setCreateTime(r.getCreateTime());

        User u = userMapper.selectById(r.getUserId());
        if (u != null) {
            vo.setUserNickname(u.getNickname());
            vo.setUserAvatar(u.getAvatarKey());
        }
        return vo;
    }
}
