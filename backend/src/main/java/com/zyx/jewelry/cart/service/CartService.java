package com.zyx.jewelry.cart.service;

import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginUser;
import com.zyx.jewelry.common.UserContextHolder;
import com.zyx.jewelry.model.CartItem;
import com.zyx.jewelry.model.Product;
import com.zyx.jewelry.model.ProductSku;
import com.zyx.jewelry.product.service.ProductService;
import com.zyx.jewelry.repository.CartItemRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    @Transactional
    public Map<String, Object> addCartItem(Long skuId, Integer quantity, String engravingText, String sizeRemark, String materialRemark) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品数量必须大于 0");
        }
        ProductSku sku = productService.getEnabledSku(skuId);
        Product product = productService.getOnSaleProduct(sku.getProductId());
        CartItem cartItem = cartItemRepository.findByUserIdAndSkuId(loginUser.id(), skuId).orElseGet(CartItem::new);
        cartItem.setUserId(loginUser.id());
        cartItem.setSkuId(skuId);
        cartItem.setProductId(product.getId());
        cartItem.setProductName(product.getName());
        cartItem.setSkuSummary(productService.buildSkuSummary(sku));
        cartItem.setCoverImage(product.getCoverImage());
        cartItem.setPrice(sku.getSalePrice());
        cartItem.setQuantity(cartItem.getId() == null ? quantity : cartItem.getQuantity() + quantity);
        cartItem.setEngravingText(engravingText);
        cartItem.setSizeRemark(sizeRemark);
        cartItem.setMaterialRemark(materialRemark);
        cartItemRepository.save(cartItem);
        return toCartItem(cartItem);
    }

    public Map<String, Object> listCartItems() {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        List<Map<String, Object>> items = cartItemRepository.findByUserIdOrderByIdDesc(loginUser.id()).stream()
            .map(this::toCartItem)
            .toList();
        BigDecimal totalAmount = items.stream()
            .map(item -> (BigDecimal) item.get("subtotalAmount"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = items.stream()
            .map(item -> (Integer) item.get("quantity"))
            .reduce(0, Integer::sum);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("totalAmount", totalAmount);
        response.put("totalQuantity", totalQuantity);
        return response;
    }

    @Transactional
    public Map<String, Object> deleteCartItem(Long cartItemId) {
        LoginUser loginUser = UserContextHolder.requireAppUser();
        return cartItemRepository.findByIdAndUserId(cartItemId, loginUser.id())
            .map(item -> {
                cartItemRepository.delete(item);
                return Map.<String, Object>of("removed", true);
            })
            .orElseGet(() -> Map.of("removed", false));
    }

    public CartItem requireOwnedCartItem(Long cartItemId, Long userId) {
        return cartItemRepository.findByIdAndUserId(cartItemId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "购物车商品不存在"));
    }

    public Map<String, Object> toCartItem(CartItem cartItem) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", cartItem.getId());
        item.put("productId", cartItem.getProductId());
        item.put("skuId", cartItem.getSkuId());
        item.put("productName", cartItem.getProductName());
        item.put("skuSummary", cartItem.getSkuSummary());
        item.put("coverImage", cartItem.getCoverImage());
        item.put("price", cartItem.getPrice());
        item.put("quantity", cartItem.getQuantity());
        item.put("engravingText", cartItem.getEngravingText());
        item.put("sizeRemark", cartItem.getSizeRemark());
        item.put("materialRemark", cartItem.getMaterialRemark());
        item.put("subtotalAmount", cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        return item;
    }
}
