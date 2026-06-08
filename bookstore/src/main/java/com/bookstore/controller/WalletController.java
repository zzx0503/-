package com.bookstore.controller;

import com.bookstore.context.UserContext;
import com.bookstore.domain.vo.wallet.WalletTransactionVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "钱包", description = "余额查询与充值")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "查询当前余额")
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance() {
        Long userId = UserContext.requireUserId();
        return Result.success(walletService.getBalance(userId));
    }

    @Operation(summary = "模拟充值")
    @PostMapping("/recharge")
    public Result<Void> recharge(@RequestParam BigDecimal amount) {
        Long userId = UserContext.requireUserId();
        walletService.recharge(userId, amount);
        return Result.success();
    }

    @Operation(summary = "查询交易明细")
    @GetMapping("/transactions")
    public Result<PageResult<WalletTransactionVO>> transactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.requireUserId();
        return Result.success(walletService.getTransactions(userId, page, size));
    }
}
