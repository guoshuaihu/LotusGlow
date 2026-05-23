package com.zyx.jewelry.admin.service;

import com.zyx.jewelry.content.service.HomeContentService;
import com.zyx.jewelry.model.AdminPermission;
import com.zyx.jewelry.model.AdminUser;
import com.zyx.jewelry.order.service.OrderService;
import com.zyx.jewelry.product.service.ProductService;
import com.zyx.jewelry.repository.BannerRepository;
import com.zyx.jewelry.repository.ContentBlockRepository;
import com.zyx.jewelry.repository.ProductRepository;
import com.zyx.jewelry.support.service.SupportService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOperationService {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final BannerRepository bannerRepository;
    private final ContentBlockRepository contentBlockRepository;
    private final SupportService supportService;
    private final HomeContentService homeContentService;
    private final AdminAccessService adminAccessService;
    private final AdminAuditService adminAuditService;

    public List<Map<String, Object>> listOrders() {
        adminAccessService.requirePermission(AdminPermission.ORDER_MANAGE);
        return orderService.listAllOrders();
    }

    public Map<String, Object> shipOrder(String orderNo, String company, String trackingNo) {
        AdminUser adminUser = adminAccessService.requirePermission(AdminPermission.ORDER_MANAGE);
        Map<String, Object> result = orderService.shipOrder(orderNo, company, trackingNo);
        adminAuditService.log(adminUser, "ORDER_SHIP", "ORDER", orderNo, "后台录入物流并发货");
        return result;
    }

    public List<Map<String, Object>> listProducts() {
        adminAccessService.requirePermission(AdminPermission.PRODUCT_MANAGE);
        return productRepository.findAll().stream().map(product -> productService.getAdminProductDetail(product.getId())).toList();
    }

    public Map<String, Object> createCategory(String name, String icon, Integer sortOrder) {
        AdminUser adminUser = adminAccessService.requirePermission(AdminPermission.PRODUCT_MANAGE);
        Map<String, Object> result = productService.createCategory(name, icon, sortOrder);
        adminAuditService.log(adminUser, "CATEGORY_CREATE", "CATEGORY", String.valueOf(result.get("id")), "新增商品分类");
        return result;
    }

    public Map<String, Object> createProduct(ProductService.AdminCreateProductCommand command) {
        AdminUser adminUser = adminAccessService.requirePermission(AdminPermission.PRODUCT_MANAGE);
        Map<String, Object> result = productService.createProduct(command);
        adminAuditService.log(adminUser, "PRODUCT_CREATE", "PRODUCT", String.valueOf(result.get("id")), "新增商品和 SKU");
        return result;
    }

    public Map<String, Object> adjustProductStock(Long productId, Long skuId, Integer newStock, String reason) {
        AdminUser adminUser = adminAccessService.requirePermission(AdminPermission.PRODUCT_MANAGE);
        Map<String, Object> result = productService.adjustStock(productId, skuId, newStock, reason, adminUser.getId(), adminUser.getUsername());
        adminAuditService.log(adminUser, "STOCK_ADJUST", "SKU", String.valueOf(skuId), "库存调整为 " + newStock);
        return result;
    }

    public Map<String, Object> getHomeConfig() {
        adminAccessService.requirePermission(AdminPermission.CONTENT_MANAGE);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("banners", bannerRepository.findAllByOrderBySortOrderAsc());
        config.put("contentBlocks", contentBlockRepository.findAllByOrderByIdAsc());
        config.put("preview", homeContentService.loadHomePage());
        return config;
    }

    public List<Map<String, Object>> listCustomRequests() {
        adminAccessService.requirePermission(AdminPermission.USER_VIEW);
        return supportService.listCustomRequests();
    }

    public List<Map<String, Object>> listAfterSales() {
        adminAccessService.requirePermission(AdminPermission.AFTER_SALE_MANAGE);
        return supportService.listAfterSalesForAdmin();
    }

    public Map<String, Object> auditAfterSale(Long afterSaleId, boolean approved, String remark) {
        AdminUser adminUser = adminAccessService.requirePermission(AdminPermission.AFTER_SALE_MANAGE);
        Map<String, Object> result = supportService.auditAfterSale(afterSaleId, adminUser.getId(), approved, remark);
        adminAuditService.log(adminUser, approved ? "AFTER_SALE_APPROVE" : "AFTER_SALE_REJECT", "AFTER_SALE", String.valueOf(afterSaleId), remark);
        return result;
    }
}
