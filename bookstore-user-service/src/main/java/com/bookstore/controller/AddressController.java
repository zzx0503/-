package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.response.Result;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.service.AddressService;
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

    @GetMapping
    public Result<List<AddressDTO>> list() {
        return Result.success(addressService.list(UserContext.requireUserId()));
    }

    @PostMapping
    public Result<AddressDTO> create(@Valid @RequestBody AddressFormDTO dto) {
        return Result.success(addressService.create(UserContext.requireUserId(), dto));
    }

    @PutMapping("/{id}")
    public Result<AddressDTO> update(@PathVariable Long id,
                                    @Valid @RequestBody AddressFormDTO dto) {
        return Result.success(addressService.update(UserContext.requireUserId(), id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.requireUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.requireUserId(), id);
        return Result.success();
    }
}
