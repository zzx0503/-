package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.dto.coupon.CouponTemplateDTO;
import com.bookstore.domain.vo.coupon.CouponTemplateVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.CouponTemplateService;
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

@Tag(name = "优惠券模板管理", description = "后台优惠券模板")
@RestController
@RequestMapping("/api/admin/coupon-templates")
@RequiredArgsConstructor
@AdminRequired
public class CouponTemplateAdminController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "创建优惠券模板")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponTemplateDTO dto) {
        return Result.success(couponTemplateService.create(dto));
    }

    @Operation(summary = "修改优惠券模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponTemplateDTO dto) {
        couponTemplateService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除优惠券模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponTemplateService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发放优惠券")
    @PostMapping("/{id}/issue")
    public Result<Void> issue(@PathVariable Long id) {
        couponTemplateService.issue(id);
        return Result.success();
    }

    @Operation(summary = "结束优惠券活动")
    @PostMapping("/{id}/end")
    public Result<Void> end(@PathVariable Long id) {
        couponTemplateService.end(id);
        return Result.success();
    }

    @Operation(summary = "查询优惠券模板详情")
    @GetMapping("/{id}")
    public Result<CouponTemplateVO> detail(@PathVariable Long id) {
        return Result.success(couponTemplateService.detail(id));
    }

    @Operation(summary = "查询优惠券模板列表")
    @GetMapping
    public Result<PageResult<CouponTemplateVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(couponTemplateService.listAdmin(status, page, size));
    }
}
