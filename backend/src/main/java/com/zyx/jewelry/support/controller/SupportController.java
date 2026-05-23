package com.zyx.jewelry.support.controller;

import com.zyx.jewelry.common.ApiResponse;
import com.zyx.jewelry.model.Address;
import com.zyx.jewelry.model.CustomRequest;
import com.zyx.jewelry.support.service.SupportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/addresses")
    public ApiResponse<List<Map<String, Object>>> listAddresses() {
        return ApiResponse.success(supportService.listAddresses());
    }

    @PostMapping("/addresses")
    public ApiResponse<Map<String, Object>> addAddress(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(supportService.addAddress(toAddress(request)));
    }

    @PutMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> updateAddress(@PathVariable Long addressId,
                                                          @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(supportService.updateAddress(addressId, toAddress(request)));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Map<String, Object>> deleteAddress(@PathVariable Long addressId) {
        return ApiResponse.success(supportService.deleteAddress(addressId));
    }

    @PostMapping("/favorites/{productId}")
    public ApiResponse<Map<String, Object>> toggleFavorite(@PathVariable Long productId) {
        return ApiResponse.success(supportService.toggleFavorite(productId));
    }

    @GetMapping("/favorites")
    public ApiResponse<List<Map<String, Object>>> listFavorites() {
        return ApiResponse.success(supportService.listFavorites());
    }

    @PostMapping("/custom-requests")
    public ApiResponse<Map<String, Object>> createCustomRequest(@Valid @RequestBody CustomRequestRequest request) {
        CustomRequest customRequest = new CustomRequest();
        customRequest.setProductId(request.productId());
        customRequest.setContactName(request.contactName());
        customRequest.setContactPhone(request.contactPhone());
        customRequest.setEngravingText(request.engravingText());
        customRequest.setSizeRemark(request.sizeRemark());
        customRequest.setMaterialRemark(request.materialRemark());
        customRequest.setRemark(request.remark());
        return ApiResponse.success(supportService.createCustomRequest(customRequest));
    }

    @PostMapping({"/after-sales", "/aftersales"})
    public ApiResponse<Map<String, Object>> createAfterSale(@Valid @RequestBody AfterSaleRequest request) {
        return ApiResponse.success(supportService.createAfterSale(
            request.orderNo(),
            request.orderItemId(),
            request.reason(),
            request.description()
        ));
    }

    @GetMapping("/after-sales")
    public ApiResponse<List<Map<String, Object>>> listAfterSales() {
        return ApiResponse.success(supportService.listAfterSalesForUser());
    }

    private Address toAddress(AddressRequest request) {
        Address address = new Address();
        address.setReceiverName(request.receiverName());
        address.setReceiverPhone(request.receiverPhone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetailAddress(request.detailAddress());
        address.setIsDefault(request.isDefault());
        return address;
    }

    public record AddressRequest(
        @NotBlank(message = "收件人不能为空") String receiverName,
        @NotBlank(message = "手机号不能为空") String receiverPhone,
        @NotBlank(message = "省份不能为空") String province,
        @NotBlank(message = "城市不能为空") String city,
        @NotBlank(message = "区县不能为空") String district,
        @NotBlank(message = "详细地址不能为空") String detailAddress,
        Boolean isDefault
    ) {
    }

    public record CustomRequestRequest(
        @NotNull(message = "商品不能为空") Long productId,
        @NotBlank(message = "联系人不能为空") String contactName,
        @NotBlank(message = "联系电话不能为空") String contactPhone,
        String engravingText,
        String sizeRemark,
        String materialRemark,
        String remark
    ) {
    }

    public record AfterSaleRequest(
        @NotBlank(message = "订单号不能为空") String orderNo,
        Long orderItemId,
        @NotBlank(message = "售后原因不能为空") String reason,
        String description
    ) {
    }
}
