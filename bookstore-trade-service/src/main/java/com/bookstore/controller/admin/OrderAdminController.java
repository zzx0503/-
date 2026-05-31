package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.vo.order.OrderVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单管理", description = "后台订单管理")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
@AdminRequired
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.listAllOrders(status, page, size));
    }

    @PutMapping("/{orderNo}/ship")
    public Result<Void> ship(@PathVariable String orderNo) {
        orderService.shipOrder(orderNo);
        return Result.success();
    }
}
