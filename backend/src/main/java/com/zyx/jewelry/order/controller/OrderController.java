package com.zyx.jewelry.order.controller;

import com.zyx.jewelry.common.ApiResponse;
import com.zyx.jewelry.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/app/orders")
    public ApiResponse<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrderFromCart(
            request.addressId(),
            request.cartItemIds(),
            request.buyerRemark()
        ));
    }

    @PostMapping("/api/app/orders/direct")
    public ApiResponse<Map<String, Object>> createDirectOrder(@Valid @RequestBody DirectOrderRequest request) {
        return ApiResponse.success(orderService.createDirectOrder(request.addressId(), request.skuId(), request.quantity()));
    }

    @GetMapping("/api/app/orders")
    public ApiResponse<List<Map<String, Object>>> listOrders() {
        return ApiResponse.success(orderService.listOrders());
    }

    @GetMapping("/api/app/orders/{orderNo}")
    public ApiResponse<Map<String, Object>> getOrder(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.getOrderDetail(orderNo));
    }

    @PostMapping("/api/app/orders/{orderNo}/pay")
    public ApiResponse<Map<String, Object>> createPayment(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.createPayment(orderNo));
    }

    @PostMapping("/api/app/orders/{orderNo}/confirm")
    public ApiResponse<Map<String, Object>> confirmReceipt(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.confirmReceipt(orderNo));
    }

    @PostMapping("/api/payments/notify")
    public ApiResponse<Map<String, Object>> paymentNotify(@Valid @RequestBody PaymentNotifyRequest request) {
        return ApiResponse.success(orderService.handlePaymentNotify(
            request.paymentNo(),
            request.transactionId(),
            request.paidAmount()
        ));
    }

    public record CreateOrderRequest(
        @NotNull(message = "收货地址不能为空") Long addressId,
        @NotEmpty(message = "购物车商品不能为空") List<Long> cartItemIds,
        String buyerRemark
    ) {
    }

    public record DirectOrderRequest(
        @NotNull(message = "收货地址不能为空") Long addressId,
        @NotNull(message = "商品规格不能为空") Long skuId,
        @NotNull(message = "购买数量不能为空") @Min(value = 1, message = "购买数量必须大于 0") Integer quantity
    ) {
    }

    public record PaymentNotifyRequest(
        @NotBlank(message = "支付单号不能为空") String paymentNo,
        @NotBlank(message = "微信流水号不能为空") String transactionId,
        @NotNull(message = "支付金额不能为空") BigDecimal paidAmount
    ) {
    }
}
