package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.api.book.client.BookClient;
import com.bookstore.domain.po.CartItem;
import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.po.UserCoupon;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.domain.vo.coupon.CouponSelectionVO;
import com.bookstore.domain.vo.coupon.UserCouponVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CartItemMapper;
import com.bookstore.mapper.CouponTemplateMapper;
import com.bookstore.mapper.UserCouponMapper;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CouponCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponCalculatorServiceImpl implements CouponCalculatorService {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CartItemMapper cartItemMapper;
    private final BookClient bookServiceClient;

    @Override
    public BigDecimal calcDiscount(BigDecimal totalAmount, CouponTemplate template) {
        if (totalAmount == null || template == null) {
            return BigDecimal.ZERO;
        }
        if (template.getThreshold() != null && totalAmount.compareTo(template.getThreshold()) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = template.getDiscountValue() == null ? BigDecimal.ZERO : template.getDiscountValue();
        BigDecimal discount;
        switch (template.getType()) {
            case CouponTemplateServiceImpl.TYPE_FULL_REDUCE:
            case CouponTemplateServiceImpl.TYPE_AMOUNT:
                discount = v;
                break;
            case CouponTemplateServiceImpl.TYPE_DISCOUNT:
                BigDecimal rate = v;
                if (rate.compareTo(BigDecimal.ONE) >= 0 || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    return BigDecimal.ZERO;
                }
                discount = totalAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
                break;
            default:
                return BigDecimal.ZERO;
        }
        BigDecimal cap = totalAmount.subtract(new BigDecimal("0.01"));
        if (discount.compareTo(cap) > 0) {
            discount = cap.max(BigDecimal.ZERO);
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount;
    }

    @Override
    public boolean isUsable(BigDecimal totalAmount, CouponTemplate template) {
        if (totalAmount == null || template == null) {
            return false;
        }
        if (template.getThreshold() != null && totalAmount.compareTo(template.getThreshold()) < 0) {
            return false;
        }
        return calcDiscount(totalAmount, template).compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public CouponSelectionVO findBest(Long userId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "购物车项不能为空");
        }
        List<CartItem> cartItems = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .in(CartItem::getId, cartItemIds)
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDeleted, 0)
        );
        if (cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        List<Long> bookIds = cartItems.stream().map(CartItem::getBookId).distinct().collect(Collectors.toList());
        Map<Long, BookDetailDTO> bookMap = bookIds.stream()
            .map(id -> {
                Result<BookDetailDTO> r = bookServiceClient.getBook(id);
                return r != null && r.getCode() == ResultCode.SUCCESS.getCode() ? r.getData() : null;
            })
            .filter(b -> b != null)
            .collect(Collectors.toMap(BookDetailDTO::getId, b -> b));
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            BookDetailDTO b = bookMap.get(ci.getBookId());
            if (b == null) {
                throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
            }
            totalAmount = totalAmount.add(b.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        return findBestByAmount(userId, totalAmount);
    }

    @Override
    public CouponSelectionVO findBestByAmount(Long userId, BigDecimal totalAmount) {
        CouponSelectionVO vo = new CouponSelectionVO();
        vo.setTotalAmount(totalAmount);
        vo.setDiscountAmount(BigDecimal.ZERO);
        vo.setPayAmount(totalAmount);
        vo.setUsableCoupons(List.of());

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return vo;
        }

        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> coupons = userCouponMapper.selectList(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponServiceImpl.STATUS_UNUSED)
                .gt(UserCoupon::getExpireTime, now)
                .eq(UserCoupon::getDeleted, 0)
        );
        if (coupons.isEmpty()) {
            return vo;
        }
        List<Long> templateIds = coupons.stream().map(UserCoupon::getTemplateId).distinct().collect(Collectors.toList());
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
            new LambdaQueryWrapper<CouponTemplate>().in(CouponTemplate::getId, templateIds)
        );
        Map<Long, CouponTemplate> tmap = templates.stream().collect(Collectors.toMap(CouponTemplate::getId, x -> x));

        List<UserCouponVO> usable = new ArrayList<>();
        UserCoupon best = null;
        CouponTemplate bestTpl = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (UserCoupon uc : coupons) {
            CouponTemplate t = tmap.get(uc.getTemplateId());
            if (t == null || !isUsable(totalAmount, t)) {
                continue;
            }
            BigDecimal d = calcDiscount(totalAmount, t);
            UserCouponVO uvo = new UserCouponVO();
            uvo.setId(uc.getId());
            uvo.setTemplateId(uc.getTemplateId());
            uvo.setName(t.getName());
            uvo.setType(t.getType());
            uvo.setThreshold(t.getThreshold());
            uvo.setDiscountValue(d);
            uvo.setCode(uc.getCode());
            uvo.setStatus(uc.getStatus());
            uvo.setExpireTime(uc.getExpireTime());
            uvo.setCreateTime(uc.getCreateTime());
            uvo.setDescription(t.getDescription());
            usable.add(uvo);
            if (d.compareTo(bestDiscount) > 0) {
                bestDiscount = d;
                best = uc;
                bestTpl = t;
            }
        }
        usable.sort(Comparator.comparing((UserCouponVO u) ->
            calcDiscount(totalAmount, tmap.get(u.getTemplateId()))).reversed());
        vo.setUsableCoupons(usable);

        if (best != null) {
            UserCouponVO bvo = new UserCouponVO();
            bvo.setId(best.getId());
            bvo.setTemplateId(best.getTemplateId());
            bvo.setName(bestTpl.getName());
            bvo.setType(bestTpl.getType());
            bvo.setThreshold(bestTpl.getThreshold());
            bvo.setDiscountValue(bestDiscount);
            bvo.setCode(best.getCode());
            bvo.setStatus(best.getStatus());
            bvo.setExpireTime(best.getExpireTime());
            bvo.setCreateTime(best.getCreateTime());
            bvo.setDescription(bestTpl.getDescription());
            vo.setBestCoupon(bvo);
            vo.setDiscountAmount(bestDiscount);
            vo.setPayAmount(totalAmount.subtract(bestDiscount).max(new BigDecimal("0.01")));
        }
        return vo;
    }
}
