package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.domain.dto.category.CategoryFormDTO;
import com.bookstore.domain.po.Category;
import com.bookstore.domain.vo.category.CategoryTreeVO;
import com.bookstore.domain.vo.category.CategoryVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CategoryMapper;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    @Cacheable(cacheNames = "category:tree")
    public List<CategoryTreeVO> listTree() {
        List<Category> rows = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .eq(Category::getDeleted, 0)
                .orderByAsc(Category::getSort)
        );

        Map<Long, CategoryTreeVO> voMap = rows.stream().collect(Collectors.toMap(
            Category::getId,
            c -> {
                CategoryTreeVO vo = new CategoryTreeVO();
                vo.setId(c.getId());
                vo.setName(c.getName());
                vo.setIconKey(c.getIconKey());
                vo.setSort(c.getSort());
                vo.setStatus(c.getStatus());
                vo.setChildren(new ArrayList<>());
                return vo;
            }
        ));

        List<CategoryTreeVO> roots = new ArrayList<>();
        for (Category c : rows) {
            CategoryTreeVO vo = voMap.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryTreeVO parent = voMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "category:tree", allEntries = true)
    public CategoryVO create(CategoryFormDTO dto) {
        Category c = new Category();
        c.setName(dto.getName());
        c.setParentId(dto.getParentId());
        c.setIconKey(dto.getIconKey());
        c.setSort(dto.getSort());
        c.setStatus(dto.getStatus());
        categoryMapper.insert(c);
        return toVO(c);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "category:tree", allEntries = true)
    public CategoryVO update(Long id, CategoryFormDTO dto) {
        Category c = categoryMapper.selectById(id);
        if (c == null || c.getDeleted() == 1) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        c.setName(dto.getName());
        c.setParentId(dto.getParentId());
        c.setIconKey(dto.getIconKey());
        c.setSort(dto.getSort());
        c.setStatus(dto.getStatus());
        categoryMapper.updateById(c);
        return toVO(c);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "category:tree", allEntries = true)
    public void delete(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null || c.getDeleted() == 1) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        long childCount = categoryMapper.selectCount(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id)
                .eq(Category::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "存在子分类,无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "category:tree", allEntries = true)
    public void toggleStatus(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null || c.getDeleted() == 1) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        c.setStatus(c.getStatus() == 1 ? 0 : 1);
        categoryMapper.updateById(c);
    }

    private CategoryVO toVO(Category c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setParentId(c.getParentId());
        vo.setIconKey(c.getIconKey());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
