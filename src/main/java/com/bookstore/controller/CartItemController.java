package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.cart.CartItemFormDTO;
import com.bookstore.domain.vo.cart.CartItemVO;
import com.bookstore.response.Result;
import com.bookstore.service.CartItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车控制器
 * 提供购物车增删改查功能，需要登录才能访问
 */
@Tag(name = "购物车", description = "购物车管理")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@LoginRequired
public class CartItemController {

    private final CartItemService cartItemService;

    /**
     * 获取当前用户的购物车列表
     * @return 购物车项列表
     */
    @GetMapping
    public Result<List<CartItemVO>> list() {
        return Result.success(cartItemService.listCart(UserContext.requireUserId()));
    }

    /**
     * 添加商品到购物车
     * 如果商品已存在，则增加数量
     * @param dto 购物车项信息（图书ID、数量）
     * @return 添加后的购物车项
     */
    @PostMapping
    public Result<CartItemVO> add(@Valid @RequestBody CartItemFormDTO dto) {
        return Result.success(cartItemService.addToCart(UserContext.requireUserId(), dto));
    }

    /**
     * 更新购物车商品数量
     * @param id 购物车项ID
     * @param quantity 新数量
     * @return 更新后的购物车项
     */
    @PutMapping("/{id}/quantity")
    public Result<CartItemVO> updateQuantity(@PathVariable Long id,
                                             @RequestParam Integer quantity) {
        return Result.success(cartItemService.updateQuantity(UserContext.requireUserId(), id, quantity));
    }

    /**
     * 更新购物车商品选中状态
     * 用于结算时选择哪些商品参与下单
     * @param id 购物车项ID
     * @param selected 是否选中（1=选中，0=未选中）
     * @return 操作结果
     */
    @PutMapping("/{id}/selected")
    public Result<Void> updateSelected(@PathVariable Long id,
                                       @RequestParam Integer selected) {
        cartItemService.updateSelected(UserContext.requireUserId(), id, selected);
        return Result.success();
    }

    /**
     * 删除购物车中的单个商品
     * @param id 购物车项ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartItemService.delete(UserContext.requireUserId(), id);
        return Result.success();
    }

    /**
     * 清空购物车
     * 删除当前用户的所有购物车项
     * @return 操作结果
     */
    @DeleteMapping
    public Result<Void> clear() {
        cartItemService.clear(UserContext.requireUserId());
        return Result.success();
    }
}
