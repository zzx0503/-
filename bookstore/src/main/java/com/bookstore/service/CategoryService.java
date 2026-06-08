package com.bookstore.service;

import com.bookstore.domain.vo.category.CategoryTreeVO;
import com.bookstore.domain.vo.category.CategoryVO;
import com.bookstore.domain.dto.category.CategoryFormDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryTreeVO> listTree();

    CategoryVO create(CategoryFormDTO dto);

    CategoryVO update(Long id, CategoryFormDTO dto);

    void delete(Long id);

    void toggleStatus(Long id);
}
