package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.client.BookServiceClient;
import com.bookstore.client.UserServiceClient;
import com.bookstore.domain.dto.order.CreateOrderDTO;
import com.bookstore.domain.dto.order.PayOrderDTO;
import com.bookstore.domain.po.CartItem;
import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.po.OrderItem;
import com.bookstore.domain.po.OrderMain;
import com.bookstore.domain.po.UserCoupon;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.api.trade.dto.OrderItemDTO;
import com.bookstore.domain.vo.order.OrderVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CartItemMapper;
import com.bookstore.mapper.CouponTemplateMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMainMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CouponCalculatorService;
import com.bookstore.service.OrderService;
import com.bookstore.service.UserCouponService;
import com.bookstore.utils.OssUrlBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final UserCouponService userCouponService;
    private final CouponCalculatorService couponCalculatorService;
    private final OssUrlBuilder ossUrlBuilder;
    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;

    private static final String ORDER_STATUS_PENDING = "PENDING_PAY";
    private static final String ORDER_STATUS_PAID = "PAID";
    private static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    private <T> T unwrap(Result<T> result) {
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            String msg = result != null && result.getMsg() != null ? result.getMsg() : ResultCode.BIZ_ERROR.getDefaultMsg();
            throw new BusinessException(ResultCode.BIZ_ERROR, msg);
        }
        return result.getData();
    }

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderDTO dto) {
        if (dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }

        List<CartItem> cartItems = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .in(CartItem::getId, dto.getCartItemIds())
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDeleted, 0)
        );
        if (cartItems.size() != dto.getCartItemIds().size()) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        cartItems.sort(java.util.Comparator.comparing(CartItem::getBookId));

        AddressVO address = unwrap(userServiceClient.getAddress(dto.getAddressId()));
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BookDetailVO book = unwrap(bookServiceClient.getBook(item.getBookId()));
            if (book == null || book.getDeleted() == 1 || book.getStatus() != 1) {
                throw new BusinessException(ResultCode.BOOK_NOT_FOUND, "购物车中的图书已下架");
            }
            Result<Void> deductResult = bookServiceClient.deductStock(book.getId(), item.getQuantity());
            if (deductResult == null || deductResult.getCode() != ResultCode.SUCCESS.getCode()) {
                String msg = deductResult != null && deductResult.getMsg() != null
                    ? deductResult.getMsg()
                    : String.format("《%s》库存不足", book.getTitle());
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, msg);
            }
            totalAmount = totalAmount.add(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        String orderNo = generateOrderNo();
        String addressSnapshot = toJsonSnapshot(address);

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = totalAmount;
        Long couponId = dto.getUserCouponId();
        if (couponId != null) {
            UserCoupon uc = userCouponService.lockForOrder(userId, couponId, orderNo);
            CouponTemplate t = couponTemplateMapper.selectById(uc.getTemplateId());
            if (t == null) {
                throw new BusinessException(ResultCode.COUPON_TEMPLATE_NOT_FOUND);
            }
            if (totalAmount.compareTo(t.getThreshold() == null ? BigDecimal.ZERO : t.getThreshold()) < 0) {
                userCouponService.releaseForOrder(userId, couponId, orderNo);
                throw new BusinessException(ResultCode.COUPON_THRESHOLD_NOT_MET);
            }
            discountAmount = couponCalculatorService.calcDiscount(totalAmount, t);
            payAmount = totalAmount.subtract(discountAmount).max(new BigDecimal("0.01"));
        }

        OrderMain order = new OrderMain();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(couponId);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setAddressSnapshot(addressSnapshot);
        order.setRemark(dto.getRemark());
        orderMainMapper.insert(order);

        for (CartItem item : cartItems) {
            BookDetailVO book = unwrap(bookServiceClient.getBook(item.getBookId()));
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setBookId(book.getId());
            oi.setBookTitle(book.getTitle());
            oi.setBookCover(book.getCoverKey());
            oi.setUnitPrice(book.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setSubtotal(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
        }

        cartItemMapper.deleteBatchIds(cartItems.stream().map(CartItem::getId).collect(Collectors.toList()));
        return toVO(order);
    }

    @Override
    @Transactional
    public void payOrder(Long userId, String orderNo, PayOrderDTO dto) {
        OrderMain order = requireOwnOrder(userId, orderNo);
        if (!ORDER_STATUS_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
        unwrap(userServiceClient.pay(userId, orderNo, order.getPayAmount()));

        int affected = orderMainMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, order.getId())
                .eq(OrderMain::getStatus, ORDER_STATUS_PENDING)
                .set(OrderMain::getStatus, ORDER_STATUS_PAID)
                .set(OrderMain::getPayMethod, "BALANCE")
                .set(OrderMain::getPayTime, LocalDateTime.now())
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
        if (order.getCouponId() != null) {
            userCouponService.useForOrder(userId, order.getCouponId(), orderNo);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        OrderMain order = requireOwnOrder(userId, orderNo);
        if (!ORDER_STATUS_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "只有待支付订单可以取消");
        }

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        for (OrderItem oi : items) {
            bookServiceClient.restoreStock(oi.getBookId(), oi.getQuantity());
        }

        int affected = orderMainMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, order.getId())
                .eq(OrderMain::getStatus, ORDER_STATUS_PENDING)
                .set(OrderMain::getStatus, ORDER_STATUS_CANCELLED)
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
        if (order.getCouponId() != null) {
            userCouponService.releaseForOrder(userId, order.getCouponId(), orderNo);
        }
    }

    @Override
    @Transactional
    public void confirmReceive(Long userId, String orderNo) {
        OrderMain order = requireOwnOrder(userId, orderNo);
        if (!ORDER_STATUS_SHIPPED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "只有已发货订单可以确认收货");
        }
        int affected = orderMainMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, order.getId())
                .eq(OrderMain::getStatus, ORDER_STATUS_SHIPPED)
                .set(OrderMain::getStatus, ORDER_STATUS_COMPLETED)
                .set(OrderMain::getCompleteTime, LocalDateTime.now())
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    public OrderDetailDTO detail(Long userId, String orderNo) {
        OrderMain order = requireOwnOrder(userId, orderNo);
        return toDetailVO(order);
    }

    @Override
    public PageResult<OrderVO> listMyOrders(Long userId, String status, Integer page, Integer size) {
        LambdaQueryWrapper<OrderMain> w = new LambdaQueryWrapper<OrderMain>()
            .eq(OrderMain::getUserId, userId)
            .eq(OrderMain::getDeleted, 0)
            .orderByDesc(OrderMain::getCreateTime);
        if (StringUtils.hasText(status)) {
            w.eq(OrderMain::getStatus, status);
        }
        Page<OrderMain> p = orderMainMapper.selectPage(new Page<>(page, size), w);
        List<OrderVO> vos = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Transactional
    public void shipOrder(String orderNo) {
        OrderMain order = orderMainMapper.selectOne(
            new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getOrderNo, orderNo)
        );
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!ORDER_STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "只有已支付订单可以发货");
        }
        int affected = orderMainMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, order.getId())
                .eq(OrderMain::getStatus, ORDER_STATUS_PAID)
                .set(OrderMain::getStatus, ORDER_STATUS_SHIPPED)
                .set(OrderMain::getShipTime, LocalDateTime.now())
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    public PageResult<OrderVO> listAllOrders(String status, Integer page, Integer size) {
        LambdaQueryWrapper<OrderMain> w = new LambdaQueryWrapper<OrderMain>()
            .eq(OrderMain::getDeleted, 0)
            .orderByDesc(OrderMain::getCreateTime);
        if (StringUtils.hasText(status)) {
            w.eq(OrderMain::getStatus, status);
        }
        Page<OrderMain> p = orderMainMapper.selectPage(new Page<>(page, size), w);
        List<OrderVO> vos = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    private String generateOrderNo() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        RAtomicLong counter = redissonClient.getAtomicLong("order:seq:" + datePrefix.substring(0, 8));
        if (!counter.isExists()) {
            counter.expire(java.time.Duration.ofDays(2));
        }
        long seq = counter.incrementAndGet();
        return datePrefix + String.format("%06d", seq);
    }

    private OrderMain requireOwnOrder(Long userId, String orderNo) {
        OrderMain order = orderMainMapper.selectOne(
            new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getOrderNo, orderNo)
        );
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return order;
    }

    @SneakyThrows
    private String toJsonSnapshot(AddressVO address) {
        Map<String, Object> map = Map.of(
            "receiver", address.getReceiver(),
            "phone", address.getPhone(),
            "province", address.getProvince(),
            "city", address.getCity(),
            "district", address.getDistrict(),
            "detailAddress", address.getDetailAddress()
        );
        return objectMapper.writeValueAsString(map);
    }

    private OrderVO toVO(OrderMain o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setTotalAmount(o.getTotalAmount());
        vo.setPayAmount(o.getPayAmount());
        vo.setDiscountAmount(o.getDiscountAmount());
        vo.setCouponId(o.getCouponId());
        vo.setPayMethod(o.getPayMethod());
        vo.setPayTime(o.getPayTime());
        vo.setStatus(o.getStatus());
        vo.setRemark(o.getRemark());
        vo.setShipTime(o.getShipTime());
        vo.setCompleteTime(o.getCompleteTime());
        vo.setCreateTime(o.getCreateTime());
        return vo;
    }

    private OrderDetailDTO toDetailVO(OrderMain o) {
        OrderDetailDTO vo = new OrderDetailDTO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setTotalAmount(o.getTotalAmount());
        vo.setPayAmount(o.getPayAmount());
        vo.setDiscountAmount(o.getDiscountAmount());
        vo.setCouponId(o.getCouponId());
        vo.setPayMethod(o.getPayMethod());
        vo.setPayTime(o.getPayTime());
        vo.setStatus(o.getStatus());
        vo.setRemark(o.getRemark());
        vo.setAddressSnapshot(o.getAddressSnapshot());
        vo.setShipTime(o.getShipTime());
        vo.setCompleteTime(o.getCompleteTime());
        vo.setCreateTime(o.getCreateTime());

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId())
        );
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    private OrderItemDTO toItemVO(OrderItem oi) {
        OrderItemDTO vo = new OrderItemDTO();
        vo.setId(oi.getId());
        vo.setBookId(oi.getBookId());
        vo.setBookTitle(oi.getBookTitle());
        vo.setBookCover(ossUrlBuilder.toFullUrl(oi.getBookCover()));
        vo.setUnitPrice(oi.getUnitPrice());
        vo.setQuantity(oi.getQuantity());
        vo.setSubtotal(oi.getSubtotal());
        return vo;
    }
}
