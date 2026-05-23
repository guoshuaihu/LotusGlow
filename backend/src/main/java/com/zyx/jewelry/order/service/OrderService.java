package com.zyx.jewelry.order.service;

import com.zyx.jewelry.cart.service.CartService;
import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginUser;
import com.zyx.jewelry.common.UserContextHolder;
import com.zyx.jewelry.model.Address;
import com.zyx.jewelry.model.CartItem;
import com.zyx.jewelry.model.InventoryChangeRecord;
import com.zyx.jewelry.model.OrderItem;
import com.zyx.jewelry.model.OrderStatus;
import com.zyx.jewelry.model.PaymentRecord;
import com.zyx.jewelry.model.PaymentStatus;
import com.zyx.jewelry.model.Product;
import com.zyx.jewelry.model.ProductSku;
import com.zyx.jewelry.model.TradeOrder;
import com.zyx.jewelry.product.service.ProductService;
import com.zyx.jewelry.repository.CartItemRepository;
import com.zyx.jewelry.repository.InventoryChangeRecordRepository;
import com.zyx.jewelry.repository.OrderItemRepository;
import com.zyx.jewelry.repository.PaymentRecordRepository;
import com.zyx.jewelry.repository.ProductSkuRepository;
import com.zyx.jewelry.repository.TradeOrderRepository;
import com.zyx.jewelry.support.service.SupportService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryChangeRecordRepository inventoryChangeRecordRepository;
    private final ProductService productService;
    private final CartService cartService;
    private final SupportService supportService;

    @Transactional
    public Map<String, Object> createOrderFromCart(Long addressId, List<Long> cartItemIds, String buyerRemark) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一件购物车商品");
        }
        Address address = supportService.requireAddress(addressId, loginUser.id());
        List<CartItem> cartItems = cartItemIds.stream()
            .map(cartItemId -> cartService.requireOwnedCartItem(cartItemId, loginUser.id()))
            .toList();
        return createOrder(loginUser.id(), address, buyerRemark, cartItems);
    }

    @Transactional
    public Map<String, Object> createDirectOrder(Long addressId, Long skuId, Integer quantity) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        Address address = supportService.requireAddress(addressId, loginUser.id());
        ProductSku sku = productService.getEnabledSku(skuId);
        Product product = productService.getOnSaleProduct(sku.getProductId());
        CartItem cartItem = new CartItem();
        cartItem.setUserId(loginUser.id());
        cartItem.setProductId(product.getId());
        cartItem.setSkuId(skuId);
        cartItem.setProductName(product.getName());
        cartItem.setSkuSummary(productService.buildSkuSummary(sku));
        cartItem.setCoverImage(product.getCoverImage());
        cartItem.setPrice(sku.getSalePrice());
        cartItem.setQuantity(quantity);
        return createOrder(loginUser.id(), address, null, List.of(cartItem));
    }

    public List<Map<String, Object>> listOrders() {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        return tradeOrderRepository.findByUserIdOrderByIdDesc(loginUser.id()).stream()
            .map(this::toOrderSummary)
            .toList();
    }

    public Map<String, Object> getOrderDetail(String orderNo) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        TradeOrder order = tradeOrderRepository.findByOrderNoAndUserId(orderNo, loginUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        return toOrderDetail(order);
    }

    @Transactional
    public Map<String, Object> createPayment(String orderNo) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        TradeOrder order = tradeOrderRepository.findByOrderNoAndUserId(orderNo, loginUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前订单不可发起支付");
        }
        PaymentRecord paymentRecord = paymentRecordRepository.findByOrderNo(orderNo).orElseGet(PaymentRecord::new);
        paymentRecord.setOrderId(order.getId());
        paymentRecord.setOrderNo(orderNo);
        paymentRecord.setPaymentNo(paymentRecord.getPaymentNo() == null ? generatePaymentNo() : paymentRecord.getPaymentNo());
        paymentRecord.setPrepayId("mock_prepay_" + paymentRecord.getPaymentNo());
        paymentRecord.setPaidAmount(order.getPayAmount());
        paymentRecord.setStatus(PaymentStatus.UNPAID);
        paymentRecordRepository.save(paymentRecord);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentNo", paymentRecord.getPaymentNo());
        response.put("orderNo", order.getOrderNo());
        response.put("payAmount", order.getPayAmount());
        response.put("prepayId", paymentRecord.getPrepayId());
        response.put("mockPayPayload", Map.of(
            "timeStamp", String.valueOf(System.currentTimeMillis()),
            "nonceStr", UUID.randomUUID().toString().replace("-", ""),
            "package", "prepay_id=" + paymentRecord.getPrepayId()
        ));
        return response;
    }

    @Transactional
    public Map<String, Object> handlePaymentNotify(String paymentNo, String transactionId, BigDecimal paidAmount) {
        PaymentRecord paymentRecord = paymentRecordRepository.findByPaymentNo(paymentNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "支付流水不存在"));
        TradeOrder order = tradeOrderRepository.findByOrderNo(paymentRecord.getOrderNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (paymentRecord.getStatus() == PaymentStatus.PAID) {
            return toOrderDetail(order);
        }
        paymentRecord.setStatus(PaymentStatus.PAID);
        paymentRecord.setTransactionId(transactionId);
        paymentRecord.setPaidAmount(paidAmount);
        paymentRecord.setCallbackPayload("{\"paymentNo\":\"" + paymentNo + "\",\"transactionId\":\"" + transactionId + "\"}");
        paymentRecord.setPaidAt(LocalDateTime.now());
        paymentRecordRepository.save(paymentRecord);

        order.setStatus(OrderStatus.WAITING_SHIPMENT);
        order.setPaymentTime(LocalDateTime.now());
        tradeOrderRepository.save(order);
        return toOrderDetail(order);
    }

    @Transactional
    public Map<String, Object> shipOrder(String orderNo, String company, String trackingNo) {
        TradeOrder order = tradeOrderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getStatus() != OrderStatus.WAITING_SHIPMENT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单当前状态不可发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setLogisticsCompany(company);
        order.setTrackingNo(trackingNo);
        order.setShippedTime(LocalDateTime.now());
        tradeOrderRepository.save(order);
        return toOrderDetail(order);
    }

    @Transactional
    public Map<String, Object> confirmReceipt(String orderNo) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        TradeOrder order = tradeOrderRepository.findByOrderNoAndUserId(orderNo, loginUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前订单不可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedTime(LocalDateTime.now());
        tradeOrderRepository.save(order);
        return toOrderDetail(order);
    }

    public List<Map<String, Object>> listAllOrders() {
        return tradeOrderRepository.findAll().stream().map(this::toOrderSummary).toList();
    }

    private Map<String, Object> createOrder(Long userId, Address address, String buyerRemark, List<CartItem> sourceItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<Long> cartIdsToDelete = new ArrayList<>();

        for (CartItem sourceItem : sourceItems) {
            ProductSku sku = productService.getEnabledSku(sourceItem.getSkuId());
            Product product = productService.getOnSaleProduct(sourceItem.getProductId());
            if (sku.getStock() < sourceItem.getQuantity()) {
                throw new BusinessException(ErrorCode.CONFLICT, "库存不足");
            }
            int beforeStock = sku.getStock();
            sku.setStock(sku.getStock() - sourceItem.getQuantity());
            productSkuRepository.save(sku);
            saveInventoryRecord(product.getId(), sku.getId(), beforeStock, sku.getStock(), "ORDER_CREATED", userId, "app-user");

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setSkuId(sku.getId());
            orderItem.setProductName(product.getName());
            orderItem.setSkuSummary(sourceItem.getSkuSummary());
            orderItem.setCoverImage(product.getCoverImage());
            orderItem.setSalePrice(sku.getSalePrice());
            orderItem.setQuantity(sourceItem.getQuantity());
            orderItem.setSubtotalAmount(sku.getSalePrice().multiply(BigDecimal.valueOf(sourceItem.getQuantity())));
            orderItem.setEngravingText(sourceItem.getEngravingText());
            orderItem.setSizeRemark(sourceItem.getSizeRemark());
            orderItem.setMaterialRemark(sourceItem.getMaterialRemark());
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotalAmount());

            if (sourceItem.getId() != null) {
                cartIdsToDelete.add(sourceItem.getId());
            }
        }

        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatus.WAITING_PAYMENT);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setProvince(address.getProvince());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setDetailAddress(address.getDetailAddress());
        order.setBuyerRemark(buyerRemark);
        tradeOrderRepository.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemRepository.save(orderItem);
        }
        if (!cartIdsToDelete.isEmpty()) {
            cartIdsToDelete.forEach(cartItemRepository::deleteById);
        }
        return toOrderDetail(order);
    }

    private Map<String, Object> toOrderSummary(TradeOrder order) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderNo", order.getOrderNo());
        summary.put("status", order.getStatus());
        summary.put("payAmount", order.getPayAmount());
        summary.put("createdAt", order.getCreatedAt());
        summary.put("items", orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
            .map(item -> Map.of(
                "id", item.getId(),
                "productName", item.getProductName(),
                "coverImage", item.getCoverImage(),
                "quantity", item.getQuantity(),
                "salePrice", item.getSalePrice()
            ))
            .toList());
        return summary;
    }

    private Map<String, Object> toOrderDetail(TradeOrder order) {
        Map<String, Object> detail = new LinkedHashMap<>(toOrderSummary(order));
        detail.put("receiverName", order.getReceiverName());
        detail.put("receiverPhone", order.getReceiverPhone());
        detail.put("province", order.getProvince());
        detail.put("city", order.getCity());
        detail.put("district", order.getDistrict());
        detail.put("detailAddress", order.getDetailAddress());
        detail.put("buyerRemark", order.getBuyerRemark());
        detail.put("logisticsCompany", order.getLogisticsCompany());
        detail.put("trackingNo", order.getTrackingNo());
        detail.put("paymentTime", order.getPaymentTime());
        detail.put("shippedTime", order.getShippedTime());
        detail.put("completedTime", order.getCompletedTime());
        detail.put("items", orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
            .map(item -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", item.getId());
                map.put("productId", item.getProductId());
                map.put("skuId", item.getSkuId());
                map.put("productName", item.getProductName());
                map.put("coverImage", item.getCoverImage());
                map.put("skuSummary", item.getSkuSummary());
                map.put("salePrice", item.getSalePrice());
                map.put("quantity", item.getQuantity());
                map.put("subtotalAmount", item.getSubtotalAmount());
                map.put("engravingText", item.getEngravingText());
                map.put("sizeRemark", item.getSizeRemark());
                map.put("materialRemark", item.getMaterialRemark());
                return map;
            })
            .toList());
        return detail;
    }

    private void saveInventoryRecord(Long productId, Long skuId, int beforeStock, int afterStock, String reason, Long operatorId, String operatorName) {
        InventoryChangeRecord record = new InventoryChangeRecord();
        record.setProductId(productId);
        record.setSkuId(skuId);
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setChangeReason(reason);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        inventoryChangeRecordRepository.save(record);
    }

    private String generateOrderNo() {
        return "J" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String generatePaymentNo() {
        return "P" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
