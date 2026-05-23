package com.zyx.jewelry.product.controller;

import com.zyx.jewelry.common.ApiResponse;
import com.zyx.jewelry.product.service.ProductService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Map<String, Object>> listProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId
    ) {
        return ApiResponse.success(productService.listProducts(keyword, categoryId));
    }

    @GetMapping("/{productId}")
    public ApiResponse<Map<String, Object>> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductDetail(productId));
    }
}
