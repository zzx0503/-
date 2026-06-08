package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.anno.RateLimit;
import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.seckill.SeckillBuyDTO;
import com.bookstore.domain.vo.seckill.SeckillActivityVO;
import com.bookstore.domain.vo.seckill.SeckillBuyResultVO;
import com.bookstore.domain.vo.seckill.SeckillEnqueueResultVO;
import com.bookstore.domain.vo.seckill.SeckillOrderVO;
import com.bookstore.domain.vo.seckill.SeckillQueueStatusVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.SeckillActivityService;
import com.bookstore.service.SeckillQueueService;
import com.bookstore.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@Tag(name = "秒杀", description = "用户端秒杀")
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillActivityService seckillActivityService;
    private final SeckillService seckillService;
    private final SeckillQueueService seckillQueueService;

    @Operation(summary = "查询正在进行中的秒杀活动")
    @GetMapping("/running")
    public Result<List<SeckillActivityVO>> running() {
        Long userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        return Result.success(seckillActivityService.listRunning(userId));
    }

    @Operation(summary = "查询即将开始的秒杀活动")
    @GetMapping("/upcoming")
    public Result<List<SeckillActivityVO>> upcoming() {
        Long userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        return Result.success(seckillActivityService.listUpcoming(userId));
    }

    @Operation(summary = "查询秒杀活动详情")
    @GetMapping("/activities/{id}")
    public Result<SeckillActivityVO> detail(@PathVariable Long id) {
        Long userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        return Result.success(seckillActivityService.detail(id, userId));
    }

    @Operation(summary = "秒杀下单")
    @PostMapping("/buy")
    @LoginRequired
    @RateLimit(key = "seckill:buy", qps = 5)
    public Result<SeckillEnqueueResultVO> buy(@Valid @RequestBody SeckillBuyDTO dto) {
        String requestId = seckillQueueService.enqueue(UserContext.requireUserId(), dto);
        return Result.success(new SeckillEnqueueResultVO(requestId, "PROCESSING"));
    }

    @Operation(summary = "查询秒杀队列状态")
    @GetMapping("/queue/{requestId}/status")
    @LoginRequired
    public Result<SeckillQueueStatusVO> queueStatus(@PathVariable String requestId) {
        return Result.success(seckillQueueService.queryStatus(requestId));
    }

    @Operation(summary = "秒杀订单支付")
    @PostMapping("/orders/{orderNo}/pay")
    @LoginRequired
    public Result<Void> pay(@PathVariable String orderNo) {
        seckillService.payOrder(UserContext.requireUserId(), orderNo);
        return Result.success();
    }

    @Operation(summary = "取消秒杀订单")
    @PutMapping("/orders/{orderNo}/cancel")
    @LoginRequired
    public Result<Void> cancel(@PathVariable String orderNo) {
        seckillService.cancelOrder(UserContext.requireUserId(), orderNo);
        return Result.success();
    }

    @Operation(summary = "查询秒杀订单详情")
    @GetMapping("/orders/{orderNo}")
    @LoginRequired
    public Result<SeckillOrderVO> orderDetail(@PathVariable String orderNo) {
        return Result.success(seckillService.detail(UserContext.requireUserId(), orderNo));
    }

    @Operation(summary = "查询我的秒杀订单")
    @GetMapping("/orders")
    @LoginRequired
    public Result<PageResult<SeckillOrderVO>> myOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(seckillService.listMyOrders(UserContext.requireUserId(), status, page, size));
    }
}
