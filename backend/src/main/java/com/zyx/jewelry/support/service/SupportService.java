package com.zyx.jewelry.support.service;

import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginUser;
import com.zyx.jewelry.common.UserContextHolder;
import com.zyx.jewelry.model.Address;
import com.zyx.jewelry.model.AfterSaleAuditRecord;
import com.zyx.jewelry.model.AfterSaleRequest;
import com.zyx.jewelry.model.AfterSaleStatus;
import com.zyx.jewelry.model.CustomRequest;
import com.zyx.jewelry.model.CustomRequestStatus;
import com.zyx.jewelry.model.Favorite;
import com.zyx.jewelry.model.OrderItem;
import com.zyx.jewelry.model.OrderStatus;
import com.zyx.jewelry.model.TradeOrder;
import com.zyx.jewelry.product.service.ProductService;
import com.zyx.jewelry.repository.AddressRepository;
import com.zyx.jewelry.repository.AfterSaleAuditRecordRepository;
import com.zyx.jewelry.repository.AfterSaleRequestRepository;
import com.zyx.jewelry.repository.CustomRequestRepository;
import com.zyx.jewelry.repository.FavoriteRepository;
import com.zyx.jewelry.repository.OrderItemRepository;
import com.zyx.jewelry.repository.TradeOrderRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final AddressRepository addressRepository;
    private final FavoriteRepository favoriteRepository;
    private final CustomRequestRepository customRequestRepository;
    private final AfterSaleRequestRepository afterSaleRequestRepository;
    private final AfterSaleAuditRecordRepository afterSaleAuditRecordRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    @Transactional
    public Map<String, Object> addAddress(Address address) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        address.setUserId(loginUser.id());
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            resetDefaultAddress(loginUser.id());
        }
        addressRepository.save(address);
        return toAddress(address);
    }

    @Transactional
    public Map<String, Object> updateAddress(Long addressId, Address payload) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        Address address = requireAddress(addressId, loginUser.id());
        address.setReceiverName(payload.getReceiverName());
        address.setReceiverPhone(payload.getReceiverPhone());
        address.setProvince(payload.getProvince());
        address.setCity(payload.getCity());
        address.setDistrict(payload.getDistrict());
        address.setDetailAddress(payload.getDetailAddress());
        address.setIsDefault(payload.getIsDefault());
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            resetDefaultAddress(loginUser.id());
            address.setIsDefault(true);
        }
        addressRepository.save(address);
        return toAddress(address);
    }

    @Transactional
    public Map<String, Object> deleteAddress(Long addressId) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        return addressRepository.findByIdAndUserId(addressId, loginUser.id())
            .map(address -> {
                addressRepository.delete(address);
                return Map.<String, Object>of("removed", true);
            })
            .orElseGet(() -> Map.of("removed", false));
    }

    public List<Map<String, Object>> listAddresses() {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        return addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(loginUser.id()).stream()
            .map(this::toAddress)
            .toList();
    }

    public Address requireAddress(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "收货地址不存在"));
    }

    @Transactional
    public Map<String, Object> toggleFavorite(Long productId) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        productService.getOnSaleProduct(productId);
        return favoriteRepository.findByUserIdAndProductId(loginUser.id(), productId)
            .map(favorite -> {
                favoriteRepository.delete(favorite);
                return Map.<String, Object>of("favorited", false);
            })
            .orElseGet(() -> {
                Favorite favorite = new Favorite();
                favorite.setUserId(loginUser.id());
                favorite.setProductId(productId);
                favoriteRepository.save(favorite);
                return Map.of("favorited", true);
            });
    }

    public List<Map<String, Object>> listFavorites() {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        return favoriteRepository.findByUserIdOrderByIdDesc(loginUser.id()).stream()
            .map(favorite -> productService.toProductCard(productService.getOnSaleProduct(favorite.getProductId())))
            .toList();
    }

    @Transactional
    public Map<String, Object> createCustomRequest(CustomRequest customRequest) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        productService.getOnSaleProduct(customRequest.getProductId());
        customRequest.setUserId(loginUser.id());
        customRequest.setStatus(CustomRequestStatus.PENDING_FOLLOW_UP);
        customRequestRepository.save(customRequest);
        return toCustomRequest(customRequest);
    }

    @Transactional
    public Map<String, Object> createAfterSale(String orderNo, Long orderItemId, String reason, String description) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        TradeOrder order = tradeOrderRepository.findByOrderNoAndUserId(orderNo, loginUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "订单不存在或不属于当前用户"));
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前订单状态不允许申请售后");
        }
        OrderItem orderItem = null;
        if (orderItemId != null) {
            orderItem = orderItemRepository.findByIdAndOrderId(orderItemId, order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "售后商品不存在"));
        }
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderNo(order.getOrderNo());
        request.setOrderId(order.getId());
        request.setOrderItemId(orderItem == null ? null : orderItem.getId());
        request.setUserId(loginUser.id());
        request.setReason(reason);
        request.setDescription(description);
        request.setRefundStatus("NONE");
        request.setStatus(AfterSaleStatus.PENDING);
        afterSaleRequestRepository.save(request);

        order.setStatus(OrderStatus.AFTER_SALE);
        tradeOrderRepository.save(order);
        return toAfterSale(request);
    }

    public List<Map<String, Object>> listAfterSalesForUser() {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        return afterSaleRequestRepository.findByUserIdOrderByIdDesc(loginUser.id()).stream()
            .map(this::toAfterSale)
            .toList();
    }

    public List<Map<String, Object>> listAfterSalesForAdmin() {
        return afterSaleRequestRepository.findAllByOrderByIdDesc().stream()
            .map(this::toAfterSale)
            .toList();
    }

    @Transactional
    public Map<String, Object> auditAfterSale(Long afterSaleId, Long adminUserId, boolean approved, String remark) {
        AfterSaleRequest request = afterSaleRequestRepository.findById(afterSaleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "售后申请不存在"));
        if (request.getStatus() != AfterSaleStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "售后申请已处理");
        }
        request.setAuditAdminId(adminUserId);
        request.setAuditRemark(remark);
        request.setAuditedAt(LocalDateTime.now());

        TradeOrder order = tradeOrderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));

        AfterSaleAuditRecord record = new AfterSaleAuditRecord();
        record.setAfterSaleRequestId(request.getId());
        record.setAdminUserId(adminUserId);
        record.setApproved(approved);
        record.setRemark(remark);
        afterSaleAuditRecordRepository.save(record);

        if (approved) {
            request.setStatus(AfterSaleStatus.REFUNDED);
            request.setRefundStatus("SUCCESS");
            request.setRefundedAt(LocalDateTime.now());
            order.setStatus(OrderStatus.REFUNDED);
        } else {
            request.setStatus(AfterSaleStatus.REJECTED);
            request.setRefundStatus("REJECTED");
            order.setStatus(OrderStatus.COMPLETED);
        }
        afterSaleRequestRepository.save(request);
        tradeOrderRepository.save(order);
        return toAfterSale(request);
    }

    public List<Map<String, Object>> listCustomRequests() {
        return customRequestRepository.findAll().stream().map(this::toCustomRequest).toList();
    }

    private void resetDefaultAddress(Long userId) {
        addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId).forEach(address -> {
            if (Boolean.TRUE.equals(address.getIsDefault())) {
                address.setIsDefault(false);
                addressRepository.save(address);
            }
        });
    }

    private Map<String, Object> toAddress(Address address) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", address.getId());
        item.put("receiverName", address.getReceiverName());
        item.put("receiverPhone", address.getReceiverPhone());
        item.put("province", address.getProvince());
        item.put("city", address.getCity());
        item.put("district", address.getDistrict());
        item.put("detailAddress", address.getDetailAddress());
        item.put("isDefault", address.getIsDefault());
        return item;
    }

    private Map<String, Object> toCustomRequest(CustomRequest customRequest) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", customRequest.getId());
        item.put("productId", customRequest.getProductId());
        item.put("contactName", customRequest.getContactName());
        item.put("contactPhone", customRequest.getContactPhone());
        item.put("engravingText", customRequest.getEngravingText());
        item.put("sizeRemark", customRequest.getSizeRemark());
        item.put("materialRemark", customRequest.getMaterialRemark());
        item.put("remark", customRequest.getRemark());
        item.put("status", customRequest.getStatus());
        return item;
    }

    private Map<String, Object> toAfterSale(AfterSaleRequest request) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", request.getId());
        item.put("orderNo", request.getOrderNo());
        item.put("orderId", request.getOrderId());
        item.put("orderItemId", request.getOrderItemId());
        item.put("reason", request.getReason());
        item.put("description", request.getDescription());
        item.put("status", request.getStatus());
        item.put("refundStatus", request.getRefundStatus());
        item.put("auditRemark", request.getAuditRemark());
        item.put("auditedAt", request.getAuditedAt());
        item.put("refundedAt", request.getRefundedAt());
        item.put("createdAt", request.getCreatedAt());
        return item;
    }
}
