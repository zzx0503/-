package com.bookstore.service;

import com.bookstore.domain.vo.favorite.FavoriteVO;
import com.bookstore.response.PageResult;

public interface FavoriteService {

    void addFavorite(Long userId, Long bookId);

    void removeFavorite(Long userId, Long bookId);

    boolean isFavorited(Long userId, Long bookId);

    PageResult<FavoriteVO> listFavorites(Long userId, Integer page, Integer size);
}
