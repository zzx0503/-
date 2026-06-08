package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.dto.seckill.SeckillActivityDTO;
import com.bookstore.domain.vo.seckill.SeckillActivityVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.SeckillActivityService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "创建秒杀活动")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SeckillActivityDTO dto) {
        return Result.success(seckillActivityService.create(dto));
    }

    @Operation(summary = "修改秒杀活动")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SeckillActivityDTO dto) {
        seckillActivityService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除秒杀活动")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seckillActivityService.delete(id);
        return Result.success();
    }

    @Operation(summary = "开始秒杀活动")
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        seckillActivityService.start(id);
        return Result.success();
    }

    @Operation(summary = "结束秒杀活动")
    @PostMapping("/{id}/end")
    public Result<Void> end(@PathVariable Long id) {
        seckillActivityService.end(id);
        return Result.success();
    }

    @Operation(summary = "查询秒杀活动详情")
    @GetMapping("/{id}")
    public Result<SeckillActivityVO> detail(@PathVariable Long id) {
        return Result.success(seckillActivityService.detail(id, null));
    }

    @Operation(summary = "查询秒杀活动列表")
    @GetMapping
    public Result<PageResult<SeckillActivityVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(seckillActivityService.listAdmin(status, page, size));
    }
}
