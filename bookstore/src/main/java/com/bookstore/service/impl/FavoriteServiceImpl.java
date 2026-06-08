package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.Favorite;
import com.bookstore.domain.vo.favorite.FavoriteVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.CategoryMapper;
import com.bookstore.mapper.FavoriteMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.ResultCode;
import com.bookstore.service.FavoriteService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final RedissonClient redissonClient;
    private final OssUrlBuilder ossUrlBuilder;

    private static final String REDIS_KEY_PREFIX = "favorite:user:";

    @Override
    @Transactional
    public void addFavorite(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || Integer.valueOf(1).equals(book.getDeleted())) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        Favorite exist = favoriteMapper.selectOne(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId)
                .eq(Favorite::getDeleted, 0)
        );
        if (exist != null) {
            throw new BusinessException(ResultCode.FAVORITE_ALREADY_EXISTS);
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setBookId(bookId);
        try {
            favoriteMapper.insert(f);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 可能之前逻辑删除过，尝试恢复
            int restored = favoriteMapper.restoreDeleted(userId, bookId);
            if (restored == 0) {
                throw new BusinessException(ResultCode.FAVORITE_ALREADY_EXISTS);
            }
        }
        try {
            RSet<Long> set = redissonClient.getSet(REDIS_KEY_PREFIX + userId);
            set.add(bookId);
        } catch (Exception e) {
            log.warn("Redis favorite add failed, userId={}, bookId={}", userId, bookId, e);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long bookId) {
        Favorite f = favoriteMapper.selectOne(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId)
                .eq(Favorite::getDeleted, 0)
        );
        if (f == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收藏不存在");
        }
        favoriteMapper.deleteById(f.getId());
        try {
            RSet<Long> set = redissonClient.getSet(REDIS_KEY_PREFIX + userId);
            set.remove(bookId);
        } catch (Exception e) {
            log.warn("Redis favorite remove failed, userId={}, bookId={}", userId, bookId, e);
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long bookId) {
        try {
            RSet<Long> set = redissonClient.getSet(REDIS_KEY_PREFIX + userId);
            if (set.contains(bookId)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis favorite check failed, fallback to DB", e);
        }
        return favoriteMapper.selectCount(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId)
                .eq(Favorite::getDeleted, 0)
        ) > 0;
    }

    @Override
    public PageResult<FavoriteVO> listFavorites(Long userId, Integer page, Integer size) {
        Page<Favorite> p = favoriteMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDeleted, 0)
                .orderByDesc(Favorite::getCreateTime)
        );
        List<Favorite> records = p.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(List.of(), p.getTotal(), p.getCurrent(), p.getSize());
        }

        // 批量查 book
        List<Long> bookIds = records.stream().map(Favorite::getBookId).distinct().collect(Collectors.toList());
        List<Book> books = bookMapper.selectBatchIds(bookIds);
        java.util.Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b));

        // 批量查 category
        java.util.Set<Long> catIds = books.stream()
            .map(Book::getCategoryId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        java.util.Map<Long, String> catNameMap = new java.util.HashMap<>();
        if (!catIds.isEmpty()) {
            catNameMap.putAll(
                categoryMapper.selectBatchIds(catIds).stream()
                    .collect(Collectors.toMap(com.bookstore.domain.po.Category::getId, com.bookstore.domain.po.Category::getName))
            );
        }

        List<FavoriteVO> vos = records.stream()
            .map(f -> toVO(f, bookMap.get(f.getBookId()), catNameMap))
            .collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    private FavoriteVO toVO(Favorite f, Book b, java.util.Map<Long, String> catNameMap) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(f.getId());
        vo.setBookId(f.getBookId());
        if (b != null) {
            vo.setBookTitle(b.getTitle());
            vo.setBookCover(ossUrlBuilder.toFullUrl(b.getCoverKey()));
            vo.setBookAuthor(b.getAuthor());
            vo.setBookPrice(b.getPrice());
            vo.setBookCategoryId(b.getCategoryId());
            if (b.getCategoryId() != null) {
                vo.setBookCategoryName(catNameMap.get(b.getCategoryId()));
            }
        }
        vo.setCreateTime(f.getCreateTime());
        return vo;
    }
}
