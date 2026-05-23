package com.zyx.jewelry.product.service;

import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginRole;
import com.zyx.jewelry.common.UserContextHolder;
import com.zyx.jewelry.model.Favorite;
import com.zyx.jewelry.model.InventoryChangeRecord;
import com.zyx.jewelry.model.Product;
import com.zyx.jewelry.model.ProductMedia;
import com.zyx.jewelry.model.ProductSku;
import com.zyx.jewelry.model.ProductStatus;
import com.zyx.jewelry.model.SkuStatus;
import com.zyx.jewelry.repository.CategoryRepository;
import com.zyx.jewelry.repository.FavoriteRepository;
import com.zyx.jewelry.repository.InventoryChangeRecordRepository;
import com.zyx.jewelry.repository.ProductMediaRepository;
import com.zyx.jewelry.repository.ProductRepository;
import com.zyx.jewelry.repository.ProductSkuRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMediaRepository productMediaRepository;
    private final FavoriteRepository favoriteRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryChangeRecordRepository inventoryChangeRecordRepository;

    public Map<String, Object> listProducts(String keyword, Long categoryId) {
        List<Product> products = productRepository.findByStatus(ProductStatus.ON_SALE).stream()
            .filter(product -> categoryId == null || categoryId.equals(product.getCategoryId()))
            .filter(product -> matchesKeyword(product, keyword))
            .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", products.size());
        response.put("items", products.stream().map(this::toProductCard).toList());
        return response;
    }

    public Map<String, Object> getProductDetail(Long productId) {
        Product product = getOnSaleProduct(productId);
        Map<String, Object> response = new LinkedHashMap<>(toProductCard(product));
        response.put("description", product.getDescription());
        response.put("certificateInfo", product.getCertificateInfo());
        response.put("serviceInfo", product.getServiceInfo());
        response.put("supportCustom", product.getSupportCustom());
        response.put("status", product.getStatus());
        response.put("skus", getSkuMaps(productId, false));
        response.put("media", getMediaMaps(productId));
        response.put("favorited", isFavorited(productId));
        return response;
    }

    @Transactional
    public Map<String, Object> createCategory(String name, String icon, Integer sortOrder) {
        com.zyx.jewelry.model.Category category = new com.zyx.jewelry.model.Category();
        category.setName(name);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        categoryRepository.save(category);
        return Map.of(
            "id", category.getId(),
            "name", category.getName(),
            "icon", category.getIcon(),
            "sortOrder", category.getSortOrder()
        );
    }

    @Transactional
    public Map<String, Object> createProduct(AdminCreateProductCommand command) {
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品分类不存在");
        }
        Product product = new Product();
        product.setCategoryId(command.categoryId());
        product.setName(command.name());
        product.setSubtitle(command.subtitle());
        product.setProductNo(command.productNo());
        product.setBasePrice(command.basePrice());
        product.setSalesCount(0);
        product.setDescription(command.description());
        product.setCertificateInfo(command.certificateInfo());
        product.setServiceInfo(command.serviceInfo());
        product.setSupportCustom(Boolean.TRUE.equals(command.supportCustom()));
        product.setHotFlag(Boolean.TRUE.equals(command.hotFlag()));
        product.setNewFlag(Boolean.TRUE.equals(command.newFlag()));
        product.setTagsCsv(command.tags() == null || command.tags().isEmpty() ? "" : String.join(",", command.tags()));
        product.setStatus(command.status());
        product.setCoverImage(command.media() == null || command.media().isEmpty() ? null : command.media().getFirst().mediaUrl());
        productRepository.save(product);

        if (command.media() != null) {
            for (MediaCommand mediaCommand : command.media()) {
                ProductMedia media = new ProductMedia();
                media.setProductId(product.getId());
                media.setMediaType(mediaCommand.mediaType());
                media.setMediaUrl(mediaCommand.mediaUrl());
                media.setSortOrder(mediaCommand.sortOrder());
                productMediaRepository.save(media);
            }
        }

        if (command.skus() == null || command.skus().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品至少需要一个 SKU");
        }
        for (SkuCommand skuCommand : command.skus()) {
            ProductSku sku = new ProductSku();
            sku.setProductId(product.getId());
            sku.setSkuCode(skuCommand.skuCode());
            sku.setMaterial(skuCommand.material());
            sku.setRingSize(skuCommand.ringSize());
            sku.setWeightDesc(skuCommand.weightDesc());
            sku.setSalePrice(skuCommand.salePrice());
            sku.setStock(skuCommand.stock());
            sku.setStatus(skuCommand.status());
            productSkuRepository.save(sku);
        }
        return getAdminProductDetail(product.getId());
    }

    @Transactional
    public Map<String, Object> adjustStock(Long productId, Long skuId, Integer newStock, String reason, Long operatorId, String operatorName) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        ProductSku sku = productSkuRepository.findById(skuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品规格不存在"));
        if (!product.getId().equals(sku.getProductId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 不属于当前商品");
        }
        int beforeStock = sku.getStock();
        sku.setStock(newStock);
        productSkuRepository.save(sku);

        InventoryChangeRecord record = new InventoryChangeRecord();
        record.setProductId(productId);
        record.setSkuId(skuId);
        record.setBeforeStock(beforeStock);
        record.setAfterStock(newStock);
        record.setChangeReason(reason);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        inventoryChangeRecordRepository.save(record);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("productId", productId);
        response.put("skuId", skuId);
        response.put("stock", sku.getStock());
        response.put("beforeStock", beforeStock);
        response.put("reason", reason);
        return response;
    }

    public Map<String, Object> getAdminProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", product.getId());
        response.put("categoryId", product.getCategoryId());
        response.put("name", product.getName());
        response.put("subtitle", product.getSubtitle());
        response.put("productNo", product.getProductNo());
        response.put("basePrice", product.getBasePrice());
        response.put("description", product.getDescription());
        response.put("certificateInfo", product.getCertificateInfo());
        response.put("serviceInfo", product.getServiceInfo());
        response.put("supportCustom", product.getSupportCustom());
        response.put("hotFlag", product.getHotFlag());
        response.put("newFlag", product.getNewFlag());
        response.put("tags", splitTags(product.getTagsCsv()));
        response.put("status", product.getStatus());
        response.put("coverImage", product.getCoverImage());
        response.put("media", getMediaMaps(productId));
        response.put("skus", getSkuMaps(productId, true));
        return response;
    }

    public Product getOnSaleProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品已下架");
        }
        return product;
    }

    public ProductSku getEnabledSku(Long skuId) {
        return productSkuRepository.findByIdAndStatus(skuId, SkuStatus.ENABLED)
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "商品规格不存在或已下架"));
    }

    public Map<String, Object> toProductCard(Product product) {
        BigDecimal lowestPrice = productSkuRepository.findByProductIdAndStatus(product.getId(), SkuStatus.ENABLED).stream()
            .map(ProductSku::getSalePrice)
            .min(BigDecimal::compareTo)
            .orElse(product.getBasePrice());
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", product.getId());
        card.put("name", product.getName());
        card.put("subtitle", product.getSubtitle());
        card.put("coverImage", product.getCoverImage());
        card.put("price", lowestPrice);
        card.put("basePrice", product.getBasePrice());
        card.put("salesCount", product.getSalesCount());
        card.put("categoryId", product.getCategoryId());
        card.put("supportCustom", product.getSupportCustom());
        card.put("status", product.getStatus());
        card.put("tags", splitTags(product.getTagsCsv()));
        return card;
    }

    public String buildSkuSummary(ProductSku sku) {
        return sku.getMaterial() + " / " + sku.getRingSize() + " / " + sku.getWeightDesc();
    }

    private boolean matchesKeyword(Product product, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return product.getName().toLowerCase(Locale.ROOT).contains(normalized)
            || product.getSubtitle().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private List<Map<String, Object>> getMediaMaps(Long productId) {
        return productMediaRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
            .map(media -> Map.<String, Object>of(
                "id", media.getId(),
                "mediaType", media.getMediaType(),
                "mediaUrl", media.getMediaUrl(),
                "sortOrder", media.getSortOrder()
            ))
            .toList();
    }

    private List<Map<String, Object>> getSkuMaps(Long productId, boolean includeDisabled) {
        List<ProductSku> skus = includeDisabled
            ? productSkuRepository.findByProductIdOrderByIdAsc(productId)
            : productSkuRepository.findByProductIdAndStatus(productId, SkuStatus.ENABLED);
        return skus.stream()
            .map(sku -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", sku.getId());
                map.put("skuCode", sku.getSkuCode());
                map.put("material", sku.getMaterial());
                map.put("ringSize", sku.getRingSize());
                map.put("weightDesc", sku.getWeightDesc());
                map.put("salePrice", sku.getSalePrice());
                map.put("stock", sku.getStock());
                map.put("status", sku.getStatus());
                map.put("skuSummary", buildSkuSummary(sku));
                return map;
            })
            .toList();
    }

    private boolean isFavorited(Long productId) {
        return UserContextHolder.get() != null
            && UserContextHolder.get().role() == LoginRole.APP_USER
            && favoriteRepository.findByUserIdAndProductId(UserContextHolder.get().id(), productId).map(Favorite::getId).isPresent();
    }

    private List<String> splitTags(String tagsCsv) {
        if (!StringUtils.hasText(tagsCsv)) {
            return List.of();
        }
        return List.of(tagsCsv.split(","));
    }

    public record AdminCreateProductCommand(
        Long categoryId,
        String name,
        String subtitle,
        String productNo,
        BigDecimal basePrice,
        String description,
        String certificateInfo,
        String serviceInfo,
        Boolean supportCustom,
        Boolean hotFlag,
        Boolean newFlag,
        List<String> tags,
        ProductStatus status,
        List<MediaCommand> media,
        List<SkuCommand> skus
    ) {
    }

    public record MediaCommand(
        String mediaType,
        String mediaUrl,
        Integer sortOrder
    ) {
    }

    public record SkuCommand(
        String skuCode,
        String material,
        String ringSize,
        String weightDesc,
        BigDecimal salePrice,
        Integer stock,
        SkuStatus status
    ) {
    }
}
