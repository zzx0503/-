package com.bookstore.service;

import com.bookstore.domain.dto.review.ReviewFormDTO;
import com.bookstore.domain.vo.review.ReviewVO;
import com.bookstore.response.PageResult;

public interface ReviewService {

    ReviewVO createReview(Long userId, ReviewFormDTO dto);

    PageResult<ReviewVO> listByBook(Long bookId, Integer page, Integer size);

    void deleteReview(Long userId, Long reviewId);
}
