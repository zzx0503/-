package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.Favorite;
import com.bookstore.api.book.dto.BookListDTO;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.FavoriteMapper;
import com.bookstore.service.BookRecommendationService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookRecommendationServiceImpl implements BookRecommendationService {

    private final BookMapper bookMapper;
    private final FavoriteMapper favoriteMapper;
    private final OssUrlBuilder ossUrlBuilder;

    @Override
    public List<BookListDTO> recommendForUser(Long userId, Integer limit) {
        if (limit == null || limit < 1) limit = 10;

        Set<Long> excludeBookIds = getUserInteractedBookIds(userId);

        Set<Long> categoryIds = getUserPreferredCategoryIds(userId);
        if (categoryIds.isEmpty()) {
            return hotBooks(limit, excludeBookIds);
        }

        List<Book> books = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .in(Book::getCategoryId, categoryIds)
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .notIn(!excludeBookIds.isEmpty(), Book::getId, excludeBookIds)
                .orderByDesc(Book::getRating)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + limit)
        );

        if (books.size() < limit) {
            int remaining = limit - books.size();
            Set<Long> foundIds = books.stream().map(Book::getId).collect(Collectors.toSet());
            foundIds.addAll(excludeBookIds);
            List<Book> extra = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1)
                    .eq(Book::getDeleted, 0)
                    .notIn(!foundIds.isEmpty(), Book::getId, foundIds)
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + remaining)
            );
            books.addAll(extra);
        }
        return books.stream().map(this::toListVO).collect(Collectors.toList());
    }

    @Override
    public List<BookListDTO> similarBooks(Long bookId, Integer limit) {
        if (limit == null || limit < 1) limit = 6;
        Book source = bookMapper.selectById(bookId);
        if (source == null || source.getDeleted() == 1 || source.getStatus() != 1) {
            return Collections.emptyList();
        }

        List<Book> sameCat = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getCategoryId, source.getCategoryId())
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .ne(Book::getId, bookId)
                .orderByDesc(Book::getRating)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + limit)
        );

        if (sameCat.size() >= limit) {
            return sameCat.stream().map(this::toListVO).collect(Collectors.toList());
        }

        int remaining = limit - sameCat.size();
        Set<Long> foundIds = sameCat.stream().map(Book::getId).collect(Collectors.toSet());
        foundIds.add(bookId);

        List<Book> sameAuthor = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getAuthor, source.getAuthor())
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .notIn(Book::getId, foundIds)
                .orderByDesc(Book::getRating)
                .last("LIMIT " + remaining)
        );
        sameCat.addAll(sameAuthor);

        if (sameCat.size() >= limit) {
            return sameCat.stream().map(this::toListVO).collect(Collectors.toList());
        }

        remaining = limit - sameCat.size();
        foundIds = sameCat.stream().map(Book::getId).collect(Collectors.toSet());
        foundIds.add(bookId);
        List<Book> extra = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .notIn(Book::getId, foundIds)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + remaining)
        );
        sameCat.addAll(extra);
        return sameCat.stream().map(this::toListVO).collect(Collectors.toList());
    }

    private Set<Long> getUserInteractedBookIds(Long userId) {
        Set<Long> ids = new java.util.HashSet<>();
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDeleted, 0)
        );
        favorites.forEach(f -> ids.add(f.getBookId()));
        return ids;
    }

    private Set<Long> getUserPreferredCategoryIds(Long userId) {
        Set<Long> ids = new java.util.HashSet<>();
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDeleted, 0)
        );
        for (Favorite f : favorites) {
            Book b = bookMapper.selectById(f.getBookId());
            if (b != null) ids.add(b.getCategoryId());
        }
        return ids;
    }

    private List<BookListDTO> hotBooks(Integer limit, Set<Long> excludeBookIds) {
        List<Book> books = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .notIn(!excludeBookIds.isEmpty(), Book::getId, excludeBookIds)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + limit)
        );
        return books.stream().map(this::toListVO).collect(Collectors.toList());
    }

    private BookListDTO toListVO(Book b) {
        BookListDTO vo = new BookListDTO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setAuthor(b.getAuthor());
        vo.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
        vo.setPrice(b.getPrice());
        vo.setOriginalPrice(b.getOriginalPrice());
        vo.setSalesCount(b.getSalesCount());
        vo.setRating(b.getRating());
        return vo;
    }
}
