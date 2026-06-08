package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.response.Result;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.service.AddressService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "地址", description = "收货地址管理")
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@LoginRequired
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "查询收货地址列表")
    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.success(addressService.list(UserContext.requireUserId()));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressFormDTO dto) {
        return Result.success(addressService.create(UserContext.requireUserId(), dto));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id,
                                    @Valid @RequestBody AddressFormDTO dto) {
        return Result.success(addressService.update(UserContext.requireUserId(), id, dto));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.requireUserId(), id);
        return Result.success();
    }

    @Operation(summary = "设置默认收货地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.requireUserId(), id);
        return Result.success();
    }
}
