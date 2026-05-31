package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.order.CreateOrderDTO;
import com.bookstore.domain.dto.order.PayOrderDTO;
import com.bookstore.domain.vo.order.OrderDetailVO;
import com.bookstore.domain.vo.order.OrderVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器
 * 提供订单创建、支付、取消、确认收货等功能，需要登录才能访问
 */
@Tag(name = "订单", description = "订单管理")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@LoginRequired
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     * 从购物车选中商品生成订单，支持使用优惠券
     * @param dto 订单信息（购物车项ID列表、地址ID、优惠券ID等）
     * @return 创建的订单信息
     */
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(UserContext.requireUserId(), dto));
    }

    /**
     * 支付订单
     * 支持余额支付、支付宝、微信等多种支付方式
     * @param orderNo 订单号
     * @param dto 支付信息（支付方式）
     * @return 操作结果
     */
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo,
                           @Valid @RequestBody PayOrderDTO dto) {
        orderService.payOrder(UserContext.requireUserId(), orderNo, dto);
        return Result.success();
    }

    /**
     * 取消订单
     * 只能取消待付款状态的订单
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancelOrder(UserContext.requireUserId(), orderNo);
        return Result.success();
    }

    /**
     * 确认收货
     * 用户确认收到货物，订单状态变为已完成
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PutMapping("/{orderNo}/confirm")
    public Result<Void> confirm(@PathVariable String orderNo) {
        orderService.confirmReceive(UserContext.requireUserId(), orderNo);
        return Result.success();
    }

    /**
     * 获取订单详情
     * 包含订单基本信息、商品清单、收货地址等
     * @param orderNo 订单号
     * @return 订单详细信息
     */
    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.success(orderService.detail(UserContext.requireUserId(), orderNo));
    }

    /**
     * 查询我的订单列表
     * 支持按订单状态筛选
     * @param status 订单状态（可选）：PENDING_PAY/PAID/SHIPPED/COMPLETED/CANCELLED
     * @param page 页码，从1开始
     * @param size 每页数量
     * @return 订单分页列表
     */
    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.listMyOrders(UserContext.requireUserId(), status, page, size));
    }
}
