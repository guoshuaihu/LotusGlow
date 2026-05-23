package com.zyx.jewelry.cart.controller;

import com.zyx.jewelry.cart.service.CartService;
import com.zyx.jewelry.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<Map<String, Object>> listCart() {
        return ApiResponse.success(cartService.listCartItems());
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> addCartItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addCartItem(
            request.skuId(),
            request.quantity(),
            request.engravingText(),
            request.sizeRemark(),
            request.materialRemark()
        ));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Map<String, Object>> deleteCartItem(@PathVariable Long cartItemId) {
        return ApiResponse.success(cartService.deleteCartItem(cartItemId));
    }

    public record AddCartItemRequest(
        @NotNull(message = "skuId 不能为空") Long skuId,
        @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量必须大于 0") Integer quantity,
        String engravingText,
        String sizeRemark,
        String materialRemark
    ) {
    }
}
