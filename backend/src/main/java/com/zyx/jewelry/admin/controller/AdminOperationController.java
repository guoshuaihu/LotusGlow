package com.zyx.jewelry.admin.controller;

import com.zyx.jewelry.admin.service.AdminOperationService;
import com.zyx.jewelry.common.ApiResponse;
import com.zyx.jewelry.model.ProductStatus;
import com.zyx.jewelry.model.SkuStatus;
import com.zyx.jewelry.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOperationController {

    private final AdminOperationService adminOperationService;

    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> listOrders() {
        return ApiResponse.success(adminOperationService.listOrders());
    }

    @PostMapping("/orders/{orderNo}/ship")
    public ApiResponse<Map<String, Object>> shipOrder(@PathVariable String orderNo,
                                                      @Valid @RequestBody ShipOrderRequest request) {
        return ApiResponse.success(adminOperationService.shipOrder(orderNo, request.company(), request.trackingNo()));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> listCategories() {
        return ApiResponse.success(adminOperationService.listCategories());
    }

    @GetMapping("/products")
    public ApiResponse<List<Map<String, Object>>> listProducts(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(adminOperationService.listProducts(keyword, categoryId));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<Map<String, Object>> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(adminOperationService.getProduct(productId));
    }

    @PostMapping("/categories")
    public ApiResponse<Map<String, Object>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(adminOperationService.createCategory(request.name(), request.icon(), request.sortOrder()));
    }

    @PostMapping("/products")
    public ApiResponse<Map<String, Object>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(adminOperationService.createProduct(toCreateCommand(request)));
    }

    @PutMapping("/products/{productId}")
    public ApiResponse<Map<String, Object>> updateProduct(@PathVariable Long productId,
                                                          @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(adminOperationService.updateProduct(productId, toUpdateCommand(request)));
    }

    @PutMapping("/products/{productId}/stock")
    public ApiResponse<Map<String, Object>> adjustProductStock(@PathVariable Long productId,
                                                               @Valid @RequestBody StockAdjustRequest request) {
        return ApiResponse.success(adminOperationService.adjustProductStock(
            productId,
            request.skuId(),
            request.newStock(),
            request.reason()
        ));
    }

    @GetMapping("/content/home")
    public ApiResponse<Map<String, Object>> getHomeConfig() {
        return ApiResponse.success(adminOperationService.getHomeConfig());
    }

    @GetMapping("/custom-requests")
    public ApiResponse<List<Map<String, Object>>> listCustomRequests() {
        return ApiResponse.success(adminOperationService.listCustomRequests());
    }

    @GetMapping("/after-sales")
    public ApiResponse<List<Map<String, Object>>> listAfterSales() {
        return ApiResponse.success(adminOperationService.listAfterSales());
    }

    @PostMapping("/after-sales/{afterSaleId}/audit")
    public ApiResponse<Map<String, Object>> auditAfterSale(@PathVariable Long afterSaleId,
                                                           @Valid @RequestBody AuditAfterSaleRequest request) {
        return ApiResponse.success(adminOperationService.auditAfterSale(afterSaleId, request.approved(), request.remark()));
    }

    private ProductService.AdminCreateProductCommand toCreateCommand(CreateProductRequest request) {
        return new ProductService.AdminCreateProductCommand(
            request.categoryId(),
            request.name(),
            request.subtitle(),
            request.productNo(),
            request.basePrice(),
            request.description(),
            request.certificateInfo(),
            request.serviceInfo(),
            request.supportCustom(),
            request.hotFlag(),
            request.newFlag(),
            request.tags(),
            request.status(),
            request.media() == null ? List.of() : request.media().stream()
                .map(item -> new ProductService.MediaCommand(item.mediaType(), item.mediaUrl(), item.sortOrder()))
                .toList(),
            request.skus() == null ? List.of() : request.skus().stream()
                .map(item -> new ProductService.SkuCommand(
                    item.skuCode(),
                    item.material(),
                    item.ringSize(),
                    item.weightDesc(),
                    item.salePrice(),
                    item.stock(),
                    item.status()
                ))
                .toList()
        );
    }

    private ProductService.AdminUpdateProductCommand toUpdateCommand(UpdateProductRequest request) {
        return new ProductService.AdminUpdateProductCommand(
            request.categoryId(),
            request.name(),
            request.subtitle(),
            request.productNo(),
            request.basePrice(),
            request.description(),
            request.certificateInfo(),
            request.serviceInfo(),
            request.supportCustom(),
            request.hotFlag(),
            request.newFlag(),
            request.tags(),
            request.status(),
            request.media() == null ? List.of() : request.media().stream()
                .map(item -> new ProductService.MediaCommand(item.mediaType(), item.mediaUrl(), item.sortOrder()))
                .toList(),
            request.skus() == null ? List.of() : request.skus().stream()
                .map(item -> new ProductService.AdminSkuCommand(
                    item.id(),
                    item.skuCode(),
                    item.material(),
                    item.ringSize(),
                    item.weightDesc(),
                    item.salePrice(),
                    item.stock(),
                    item.status()
                ))
                .toList()
        );
    }

    public record ShipOrderRequest(
        @NotBlank(message = "物流公司不能为空") String company,
        @NotBlank(message = "物流单号不能为空") String trackingNo
    ) {
    }

    public record CategoryRequest(
        @NotBlank(message = "分类名称不能为空") String name,
        @NotBlank(message = "分类图标不能为空") String icon,
        @NotNull(message = "排序不能为空") Integer sortOrder
    ) {
    }

    public record CreateProductRequest(
        @NotNull(message = "分类不能为空") Long categoryId,
        @NotBlank(message = "商品名称不能为空") String name,
        String subtitle,
        @NotBlank(message = "商品编号不能为空") String productNo,
        @NotNull(message = "基础价格不能为空") BigDecimal basePrice,
        String description,
        String certificateInfo,
        String serviceInfo,
        Boolean supportCustom,
        Boolean hotFlag,
        Boolean newFlag,
        List<String> tags,
        @NotNull(message = "商品状态不能为空") ProductStatus status,
        List<MediaItemRequest> media,
        List<CreateSkuItemRequest> skus
    ) {
    }

    public record UpdateProductRequest(
        @NotNull(message = "分类不能为空") Long categoryId,
        @NotBlank(message = "商品名称不能为空") String name,
        String subtitle,
        @NotBlank(message = "商品编号不能为空") String productNo,
        @NotNull(message = "基础价格不能为空") BigDecimal basePrice,
        String description,
        String certificateInfo,
        String serviceInfo,
        Boolean supportCustom,
        Boolean hotFlag,
        Boolean newFlag,
        List<String> tags,
        @NotNull(message = "商品状态不能为空") ProductStatus status,
        List<MediaItemRequest> media,
        List<UpdateSkuItemRequest> skus
    ) {
    }

    public record MediaItemRequest(
        String id,
        @NotBlank(message = "媒体类型不能为空") String mediaType,
        @NotBlank(message = "媒体地址不能为空") String mediaUrl,
        @NotNull(message = "排序不能为空") Integer sortOrder
    ) {
    }

    public record CreateSkuItemRequest(
        @NotBlank(message = "SKU 编码不能为空") String skuCode,
        @NotBlank(message = "材质不能为空") String material,
        @NotBlank(message = "规格不能为空") String ringSize,
        @NotBlank(message = "克重不能为空") String weightDesc,
        @NotNull(message = "售价不能为空") BigDecimal salePrice,
        @NotNull(message = "库存不能为空") Integer stock,
        @NotNull(message = "SKU 状态不能为空") SkuStatus status
    ) {
    }

    public record UpdateSkuItemRequest(
        Long id,
        @NotBlank(message = "SKU 编码不能为空") String skuCode,
        @NotBlank(message = "材质不能为空") String material,
        @NotBlank(message = "规格不能为空") String ringSize,
        @NotBlank(message = "克重不能为空") String weightDesc,
        @NotNull(message = "售价不能为空") BigDecimal salePrice,
        @NotNull(message = "库存不能为空") Integer stock,
        @NotNull(message = "SKU 状态不能为空") SkuStatus status
    ) {
    }

    public record StockAdjustRequest(
        @NotNull(message = "SKU 不能为空") Long skuId,
        @NotNull(message = "库存不能为空") Integer newStock,
        @NotBlank(message = "调整原因不能为空") String reason
    ) {
    }

    public record AuditAfterSaleRequest(
        @NotNull(message = "审核结果不能为空") Boolean approved,
        String remark
    ) {
    }
}
