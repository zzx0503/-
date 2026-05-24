package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.response.Result;
import com.bookstore.domain.dto.category.CategoryFormDTO;
import com.bookstore.domain.vo.category.CategoryVO;
import com.bookstore.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "分类管理", description = "后台分类管理")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
@AdminRequired
public class CategoryAdminController {

    private final CategoryService categoryService;

    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryFormDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id,
                                     @Valid @RequestBody CategoryFormDTO dto) {
        return Result.success(categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        categoryService.toggleStatus(id);
        return Result.success();
    }
}
