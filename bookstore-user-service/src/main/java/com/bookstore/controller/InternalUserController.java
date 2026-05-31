package com.bookstore.controller;

import com.bookstore.domain.po.Address;
import com.bookstore.domain.po.User;
import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.api.user.dto.UserProfileDTO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.mapper.UserMapper;
import com.bookstore.response.Result;
import com.bookstore.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final AddressMapper addressMapper;
    private final UserMapper userMapper;
    private final WalletService walletService;

    @GetMapping("/address/{id}")
    public Result<AddressDTO> getAddress(@PathVariable Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || (address.getDeleted() != null && address.getDeleted() == 1)) {
            return Result.fail(com.bookstore.response.ResultCode.NOT_FOUND);
        }
        AddressDTO vo = new AddressDTO();
        vo.setId(address.getId());
        vo.setReceiver(address.getReceiver());
        vo.setPhone(address.getPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setIsDefault(address.getIsDefault() != null && address.getIsDefault() == 1);
        vo.setFullAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        return Result.success(vo);
    }

    @PostMapping("/wallet/pay")
    public Result<Void> pay(@RequestParam Long userId,
                            @RequestParam String orderNo,
                            @RequestParam BigDecimal amount) {
        walletService.pay(userId, orderNo, amount);
        return Result.success();
    }

    @GetMapping("/user/{id}")
    public Result<UserProfileDTO> getUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            return Result.fail(com.bookstore.response.ResultCode.NOT_FOUND);
        }
        UserProfileDTO vo = new UserProfileDTO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarKey());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setRole(user.getRole());
        return Result.success(vo);
    }
}
