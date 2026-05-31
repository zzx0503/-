package com.bookstore.service;

import com.bookstore.domain.vo.wallet.WalletTransactionVO;
import com.bookstore.response.PageResult;

import java.math.BigDecimal;

public interface WalletService {

    BigDecimal getBalance(Long userId);

    void recharge(Long userId, BigDecimal amount);

    void pay(Long userId, String orderNo, BigDecimal amount);

    void refund(Long userId, String orderNo, BigDecimal amount);

    PageResult<WalletTransactionVO> getTransactions(Long userId, int page, int size);
}
