package com.bookstore.service;

import com.bookstore.domain.dto.coupon.CouponTemplateDTO;
import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.vo.coupon.AvailableCouponVO;
import com.bookstore.domain.vo.coupon.CouponTemplateVO;
import com.bookstore.response.PageResult;

import java.util.List;

public interface CouponTemplateService {

    Long create(CouponTemplateDTO dto);

    void update(Long id, CouponTemplateDTO dto);

    void delete(Long id);

    void issue(Long id);

    void end(Long id);

    CouponTemplateVO detail(Long id);

    PageResult<CouponTemplateVO> listAdmin(String status, Integer page, Integer size);

    List<AvailableCouponVO> listAvailable(Long userId);

    CouponTemplate requireById(Long id);
}
