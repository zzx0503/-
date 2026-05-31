package com.bookstore.controller;

import com.bookstore.domain.po.OrderMain;
import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.api.trade.dto.OrderItemDTO;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.bookstore.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;

    @GetMapping("/{orderNo}")
    public Result<OrderDetailDTO> getOrder(@PathVariable String orderNo) {
        OrderMain order = orderMainMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, orderNo)
                .eq(OrderMain::getDeleted, 0)
        );
        if (order == null) {
            return Result.fail(com.bookstore.response.ResultCode.NOT_FOUND);
        }

        List<com.bookstore.domain.po.OrderItem> items = orderItemMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.OrderItem>()
                .eq(com.bookstore.domain.po.OrderItem::getOrderId, order.getId())
        );

        OrderDetailDTO vo = new OrderDetailDTO();
        vo.setId(order.getId());
        vo.setUserId(order.getUserId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setCouponId(order.getCouponId());
        vo.setPayMethod(order.getPayMethod());
        vo.setPayTime(order.getPayTime());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return Result.success(vo);
    }

    private OrderItemDTO toItemVO(com.bookstore.domain.po.OrderItem item) {
        OrderItemDTO vo = new OrderItemDTO();
        vo.setId(item.getId());
        vo.setBookId(item.getBookId());
        vo.setBookTitle(item.getBookTitle());
        vo.setBookCover(item.getBookCover());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSubtotal(item.getSubtotal());
        return vo;
    }

    @GetMapping("/by-id/{id}")
    public Result<OrderDetailDTO> getOrderById(@PathVariable Long id) {
        OrderMain order = orderMainMapper.selectById(id);
        if (order == null || (order.getDeleted() != null && order.getDeleted() == 1)) {
            return Result.fail(com.bookstore.response.ResultCode.NOT_FOUND);
        }

        List<com.bookstore.domain.po.OrderItem> items = orderItemMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bookstore.domain.po.OrderItem>()
                .eq(com.bookstore.domain.po.OrderItem::getOrderId, order.getId())
        );

        OrderDetailDTO vo = new OrderDetailDTO();
        vo.setId(order.getId());
        vo.setUserId(order.getUserId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setCouponId(order.getCouponId());
        vo.setPayMethod(order.getPayMethod());
        vo.setPayTime(order.getPayTime());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return Result.success(vo);
    }
}
