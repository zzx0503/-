package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.po.User;
import com.bookstore.domain.po.WalletTransaction;
import com.bookstore.domain.vo.wallet.WalletTransactionVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.UserMapper;
import com.bookstore.mapper.WalletTransactionMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.ResultCode;
import com.bookstore.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return user.getWalletBalance() == null ? BigDecimal.ZERO : user.getWalletBalance();
    }

    @Override
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "充值金额必须大于0");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        int affected = userMapper.update(null,
            new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("wallet_balance = COALESCE(wallet_balance, 0) + " + amount.toPlainString())
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "充值失败");
        }
        BigDecimal after = userMapper.selectById(userId).getWalletBalance();
        BigDecimal before = after.subtract(amount);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType("RECHARGE");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark("模拟充值");
        walletTransactionMapper.insert(tx);
    }

    @Override
    @Transactional
    public void pay(Long userId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "支付金额必须大于0");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        BigDecimal before = user.getWalletBalance() == null ? BigDecimal.ZERO : user.getWalletBalance();
        if (before.compareTo(amount) < 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "余额不足");
        }
        BigDecimal after = before.subtract(amount);

        int affected = userMapper.update(null,
            new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .ge(User::getWalletBalance, amount)
                .set(User::getWalletBalance, after)
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "余额不足或支付失败");
        }

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setOrderNo(orderNo);
        tx.setType("PAY");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark("余额支付");
        walletTransactionMapper.insert(tx);
    }

    @Override
    public PageResult<WalletTransactionVO> getTransactions(Long userId, int page, int size) {
        LambdaQueryWrapper<WalletTransaction> w = new LambdaQueryWrapper<WalletTransaction>()
            .eq(WalletTransaction::getUserId, userId)
            .eq(WalletTransaction::getDeleted, 0)
            .orderByDesc(WalletTransaction::getCreateTime);
        Page<WalletTransaction> p = walletTransactionMapper.selectPage(new Page<>(page, size), w);
        List<WalletTransactionVO> list = p.getRecords().stream()
            .map(tx -> {
                WalletTransactionVO vo = new WalletTransactionVO();
                vo.setId(tx.getId());
                vo.setType(tx.getType());
                vo.setAmount(tx.getAmount());
                vo.setBalanceBefore(tx.getBalanceBefore());
                vo.setBalanceAfter(tx.getBalanceAfter());
                vo.setRemark(tx.getRemark());
                vo.setCreateTime(tx.getCreateTime());
                return vo;
            })
            .toList();
        return PageResult.of(list, p.getTotal(), (int) p.getCurrent(), (int) p.getSize());
    }

    @Override
    @Transactional
    public void refund(Long userId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        int affected = userMapper.update(null,
            new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("wallet_balance = COALESCE(wallet_balance, 0) + " + amount.toPlainString())
        );
        if (affected == 0) {
            return;
        }
        BigDecimal after = userMapper.selectById(userId).getWalletBalance();
        BigDecimal before = after.subtract(amount);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setOrderNo(orderNo);
        tx.setType("REFUND");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark("订单取消退款");
        walletTransactionMapper.insert(tx);
    }
}
