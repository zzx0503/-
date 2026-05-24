package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.dto.seckill.SeckillActivityDTO;
import com.bookstore.domain.vo.seckill.SeckillActivityVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.SeckillActivityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "秒杀活动管理", description = "后台秒杀")
@RestController
@RequestMapping("/api/admin/seckill-activities")
@RequiredArgsConstructor
@AdminRequired
public class SeckillAdminController {

    private final SeckillActivityService seckillActivityService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SeckillActivityDTO dto) {
        return Result.success(seckillActivityService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SeckillActivityDTO dto) {
        seckillActivityService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seckillActivityService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        seckillActivityService.start(id);
        return Result.success();
    }

    @PostMapping("/{id}/end")
    public Result<Void> end(@PathVariable Long id) {
        seckillActivityService.end(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SeckillActivityVO> detail(@PathVariable Long id) {
        return Result.success(seckillActivityService.detail(id, null));
    }

    @GetMapping
    public Result<PageResult<SeckillActivityVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(seckillActivityService.listAdmin(status, page, size));
    }
}
