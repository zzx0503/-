package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.dto.book.BookFormDTO;
import com.bookstore.domain.dto.book.BookQueryDTO;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.po.Category;
import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.domain.vo.book.BookVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.CategoryMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.ResultCode;
import com.bookstore.service.BookService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final OssUrlBuilder ossUrlBuilder;

    @Override
    public PageResult<BookListVO> list(BookQueryDTO query) {
        // list books with pagination
        LambdaQueryWrapper<Book> w = new LambdaQueryWrapper<Book>();
        w.eq(Book::getDeleted, 0).eq(Book::getStatus, 1);

        if (query.getCategoryId() != null) {
            w.eq(Book::getCategoryId, query.getCategoryId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(q -> q.like(Book::getTitle, kw)
                         .or()
                         .like(Book::getAuthor, kw)
                         .or()
                         .like(Book::getPublisher, kw));
        }
        if (query.getMinPrice() != null) {
            w.ge(Book::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            w.le(Book::getPrice, query.getMaxPrice());
        }

        String sortField = query.getSortField();
        String sortOrder = query.getSortOrder();
        boolean isDesc = "desc".equalsIgnoreCase(sortOrder);
        if ("price".equals(sortField)) {
            w.orderBy(true, !isDesc, Book::getPrice);
        } else if ("sales".equals(sortField)) {
            w.orderBy(true, !isDesc, Book::getSalesCount);
        } else if ("rating".equals(sortField)) {
            w.orderBy(true, !isDesc, Book::getRating);
        } else {
            w.orderByDesc(Book::getId);
        }

        Page<Book> page = bookMapper.selectPage(
            new Page<>(query.getPage().longValue(), query.getSize().longValue()), w
        );
        List<BookListVO> vos = page.getRecords().stream()
            .map(this::toListVO).collect(Collectors.toList());

        if (!vos.isEmpty()) {
            java.util.Set<Long> catIds = vos.stream()
                .map(BookListVO::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            if (!catIds.isEmpty()) {
                java.util.Map<Long, String> nameMap = categoryMapper.selectBatchIds(catIds).stream()
                    .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName));
                vos.forEach(vo -> vo.setCategoryName(nameMap.get(vo.getCategoryId())));
            }
        }

        return PageResult.of(vos, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    @Cacheable(cacheNames = "book:detail", key = "#id")
    public BookDetailVO detail(Long id) {
        Book b = bookMapper.selectById(id);
        if (b == null || b.getDeleted() == 1 || b.getStatus() != 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        BookDetailVO vo = toDetailVO(b);

        List<Book> related = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getCategoryId, b.getCategoryId())
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .ne(Book::getId, b.getId())
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT 4")
        );
        vo.setRelatedBooks(related.stream().map(this::toListVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Cacheable(cacheNames = "book:hot", key = "#limit != null ? #limit : 10")
    public List<BookListVO> hot(Integer limit) {
        if (limit == null || limit < 1) limit = 10;
        List<Book> rows = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .orderByDesc(Book::getSalesCount)
                .last("LIMIT " + limit)
        );
        return rows.stream().map(this::toListVO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = "book:new", key = "#limit != null ? #limit : 10")
    public List<BookListVO> newest(Integer limit) {
        if (limit == null || limit < 1) limit = 10;
        List<Book> rows = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .eq(Book::getDeleted, 0)
                .isNotNull(Book::getPublishDate)
                .orderByDesc(Book::getPublishDate)
                .last("LIMIT " + limit)
        );
        return rows.stream().map(this::toListVO).collect(Collectors.toList());
    }

    @Override
    @com.bookstore.anno.SearchHistory(type = "TEXT")
    public PageResult<BookListVO> search(String keyword, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return PageResult.of(List.of(), 0L, (long) page, (long) size);
        }
        String kw = keyword.trim();
        if (kw.length() < 2) {
            return list(simpleQuery(kw, page, size));
        }

        try {
            List<Book> rows = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                    .apply("MATCH(title, subtitle, author) AGAINST({0} IN BOOLEAN MODE)", kw)
                    .eq(Book::getStatus, 1)
                    .eq(Book::getDeleted, 0)
                    .orderByDesc(Book::getId)
            );
            if (rows.isEmpty()) {
                return list(simpleQuery(kw, page, size));
            }
            List<BookListVO> vos = rows.stream()
                .map(this::toListVO).collect(Collectors.toList());
            return manualPage(vos, page, size);
        } catch (Exception e) {
            return list(simpleQuery(kw, page, size));
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"book:hot", "book:new"}, allEntries = true)
    public BookVO create(BookFormDTO dto) {
        Book existing = bookMapper.selectByIsbn(dto.getIsbn());
        if (existing != null) {
            if (existing.getDeleted() == 0) {
                throw new BusinessException(ResultCode.BIZ_ERROR, "ISBN 已存在");
            }
            // 恢复已逻辑删除的书籍并更新字段
            existing.setDeleted(0);
            existing.setTitle(dto.getTitle());
            existing.setSubtitle(dto.getSubtitle());
            existing.setAuthor(dto.getAuthor());
            existing.setTranslator(dto.getTranslator());
            existing.setPublisher(dto.getPublisher());
            existing.setCategoryId(dto.getCategoryId());
            existing.setCoverKey(dto.getCoverKey());
            existing.setPrice(dto.getPrice());
            existing.setOriginalPrice(dto.getOriginalPrice());
            existing.setStock(dto.getStock());
            existing.setDescription(dto.getDescription());
            existing.setPublishDate(dto.getPublishDate());
            existing.setStatus(dto.getStatus());
            existing.setSalesCount(0);
            existing.setRating(new java.math.BigDecimal("5.0"));
            bookMapper.updateById(existing);
            return toVO(existing);
        }
        Book b = fromFormDTO(dto);
        bookMapper.insert(b);
        return toVO(b);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"book:detail", "book:hot", "book:new"}, key = "#id")
    public BookVO update(Long id, BookFormDTO dto) {
        Book b = bookMapper.selectById(id);
        if (b == null || b.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        checkIsbn(dto.getIsbn(), id);
        b.setIsbn(dto.getIsbn());
        b.setTitle(dto.getTitle());
        b.setSubtitle(dto.getSubtitle());
        b.setAuthor(dto.getAuthor());
        b.setTranslator(dto.getTranslator());
        b.setPublisher(dto.getPublisher());
        b.setCategoryId(dto.getCategoryId());
        b.setCoverKey(dto.getCoverKey());
        b.setPrice(dto.getPrice());
        b.setOriginalPrice(dto.getOriginalPrice());
        b.setStock(dto.getStock());
        b.setDescription(dto.getDescription());
        b.setPublishDate(dto.getPublishDate());
        b.setStatus(dto.getStatus());
        bookMapper.updateById(b);
        return toVO(b);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"book:detail", "book:hot", "book:new"}, key = "#id")
    public void delete(Long id) {
        Book b = bookMapper.selectById(id);
        if (b == null || b.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        bookMapper.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"book:detail", "book:hot", "book:new"}, key = "#id")
    public void toggleStatus(Long id) {
        Book b = bookMapper.selectById(id);
        if (b == null || b.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        b.setStatus(b.getStatus() == 1 ? 0 : 1);
        bookMapper.updateById(b);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"book:detail", "book:hot", "book:new"}, key = "#id")
    public void adjustStock(Long id, Integer delta) {
        Book b = bookMapper.selectById(id);
        if (b == null || b.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        int newStock = b.getStock() + delta;
        if (newStock < 0) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        b.setStock(newStock);
        bookMapper.updateById(b);
    }

    private void checkIsbn(String isbn, Long excludeId) {
        LambdaQueryWrapper<Book> w = new LambdaQueryWrapper<Book>()
            .eq(Book::getIsbn, isbn)
            .eq(Book::getDeleted, 0);
        if (excludeId != null) {
            w.ne(Book::getId, excludeId);
        }
        if (bookMapper.selectCount(w) > 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "ISBN 已存在");
        }
    }

    private Book fromFormDTO(BookFormDTO dto) {
        Book b = new Book();
        b.setIsbn(dto.getIsbn());
        b.setTitle(dto.getTitle());
        b.setSubtitle(dto.getSubtitle());
        b.setAuthor(dto.getAuthor());
        b.setTranslator(dto.getTranslator());
        b.setPublisher(dto.getPublisher());
        b.setCategoryId(dto.getCategoryId());
        b.setCoverKey(dto.getCoverKey());
        b.setPrice(dto.getPrice());
        b.setOriginalPrice(dto.getOriginalPrice());
        b.setStock(dto.getStock());
        b.setSalesCount(0);
        b.setRating(new java.math.BigDecimal("5.0"));
        b.setDescription(dto.getDescription());
        b.setPublishDate(dto.getPublishDate());
        b.setStatus(dto.getStatus());
        return b;
    }

    private BookListVO toListVO(Book b) {
        BookListVO vo = new BookListVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setAuthor(b.getAuthor());
        vo.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
        vo.setPrice(b.getPrice());
        vo.setOriginalPrice(b.getOriginalPrice());
        vo.setSalesCount(b.getSalesCount());
        vo.setRating(b.getRating());
        vo.setCategoryId(b.getCategoryId());
        return vo;
    }

    private BookDetailVO toDetailVO(Book b) {
        BookDetailVO vo = new BookDetailVO();
        vo.setId(b.getId());
        vo.setIsbn(b.getIsbn());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setAuthor(b.getAuthor());
        vo.setTranslator(b.getTranslator());
        vo.setPublisher(b.getPublisher());
        vo.setCategoryId(b.getCategoryId());
        Category cat = categoryMapper.selectById(b.getCategoryId());
        vo.setCategoryName(cat != null ? cat.getName() : null);
        vo.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
        vo.setPrice(b.getPrice());
        vo.setOriginalPrice(b.getOriginalPrice());
        vo.setStock(b.getStock());
        vo.setSalesCount(b.getSalesCount());
        vo.setRating(b.getRating());
        vo.setDescription(b.getDescription());
        vo.setPublishDate(b.getPublishDate());
        vo.setStatus(b.getStatus());
        return vo;
    }

    private BookVO toVO(Book b) {
        BookVO vo = new BookVO();
        vo.setId(b.getId());
        vo.setIsbn(b.getIsbn());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setAuthor(b.getAuthor());
        vo.setTranslator(b.getTranslator());
        vo.setPublisher(b.getPublisher());
        vo.setCategoryId(b.getCategoryId());
        vo.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
        vo.setPrice(b.getPrice());
        vo.setOriginalPrice(b.getOriginalPrice());
        vo.setStock(b.getStock());
        vo.setSalesCount(b.getSalesCount());
        vo.setRating(b.getRating());
        vo.setDescription(b.getDescription());
        vo.setPublishDate(b.getPublishDate());
        vo.setStatus(b.getStatus());
        vo.setCreateTime(b.getCreateTime());
        return vo;
    }

    private BookQueryDTO simpleQuery(String keyword, Integer page, Integer size) {
        BookQueryDTO q = new BookQueryDTO();
        q.setKeyword(keyword);
        q.setPage(page);
        q.setSize(size);
        return q;
    }

    private PageResult<BookListVO> manualPage(List<BookListVO> list, Integer page, Integer size) {
        int total = list.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        if (from >= total) {
            return PageResult.of(List.of(), (long) total, (long) page, (long) size);
        }
        return PageResult.of(list.subList(from, to), (long) total, (long) page, (long) size);
    }
}
