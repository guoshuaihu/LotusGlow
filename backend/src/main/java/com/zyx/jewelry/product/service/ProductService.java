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

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMediaRepository productMediaRepository;
    private final FavoriteRepository favoriteRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryChangeRecordRepository inventoryChangeRecordRepository;

    public Map<String, Object> listProducts(String keyword, Long categoryId) {
        List<Product> products = productRepository.findAll().stream()
            .filter(product -> categoryId == null || categoryId.equals(product.getCategoryId()))
            .filter(product -> matchesKeyword(product, keyword))
            .filter(product -> product.getStatus() == ProductStatus.ON_SALE)
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

    public List<Map<String, Object>> listAdminProducts(String keyword, Long categoryId, Boolean lowStockOnly, Integer stockThreshold) {
        int threshold = normalizeStockThreshold(stockThreshold);
        return productRepository.findAll().stream()
            .filter(product -> categoryId == null || categoryId.equals(product.getCategoryId()))
            .filter(product -> matchesKeyword(product, keyword))
            .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
            .map(product -> getAdminProductDetail(product.getId(), threshold))
            .filter(product -> !Boolean.TRUE.equals(lowStockOnly) || Boolean.TRUE.equals(product.get("lowStock")))
            .toList();
    }

    public List<Map<String, Object>> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
            .map(category -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", category.getId());
                map.put("name", category.getName());
                map.put("icon", category.getIcon());
                map.put("sortOrder", category.getSortOrder());
                return map;
            })
            .toList();
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
        applyProductCommand(product, command.categoryId(), command.name(), command.subtitle(), command.productNo(),
            command.basePrice(), command.description(), command.certificateInfo(), command.serviceInfo(),
            command.supportCustom(), command.hotFlag(), command.newFlag(), command.tags(), command.status(), command.media());
        product.setSalesCount(0);
        productRepository.save(product);
        replaceProductMedia(product.getId(), command.media());
        createProductSkus(product.getId(), command.skus());
        return getAdminProductDetail(product.getId());
    }

    @Transactional
    public Map<String, Object> updateProduct(Long productId, AdminUpdateProductCommand command) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品分类不存在");
        }
        applyProductCommand(product, command.categoryId(), command.name(), command.subtitle(), command.productNo(),
            command.basePrice(), command.description(), command.certificateInfo(), command.serviceInfo(),
            command.supportCustom(), command.hotFlag(), command.newFlag(), command.tags(), command.status(), command.media());
        productRepository.save(product);
        replaceProductMedia(product.getId(), command.media());
        replaceProductSkus(product.getId(), command.skus());
        return getAdminProductDetail(product.getId());
    }

    @Transactional
    public Map<String, Object> adjustStock(Long productId, Long skuId, Integer newStock, String reason, Long operatorId, String operatorName) {
        if (newStock == null || newStock < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "库存不能小于 0");
        }
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
        response.put("lowStock", isLowStockSku(sku, DEFAULT_LOW_STOCK_THRESHOLD));
        response.put("lowStockThreshold", DEFAULT_LOW_STOCK_THRESHOLD);
        return response;
    }

    public Map<String, Object> getAdminProductDetail(Long productId) {
        return getAdminProductDetail(productId, DEFAULT_LOW_STOCK_THRESHOLD);
    }

    public Map<String, Object> getAdminProductDetail(Long productId, int stockThreshold) {
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
        List<Map<String, Object>> skus = getSkuMaps(productId, true, stockThreshold);
        long lowStockSkuCount = skus.stream()
            .filter(sku -> Boolean.TRUE.equals(sku.get("lowStock")))
            .count();
        response.put("skus", skus);
        response.put("lowStock", lowStockSkuCount > 0);
        response.put("lowStockSkuCount", lowStockSkuCount);
        response.put("lowStockThreshold", stockThreshold);
        return response;
    }

    public List<Map<String, Object>> listInventoryRecords(Long productId, Long skuId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Product not found"));
        List<InventoryChangeRecord> records;
        if (skuId == null) {
            records = inventoryChangeRecordRepository.findByProductIdOrderByIdDesc(product.getId());
        } else {
            ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SKU not found"));
            if (!product.getId().equals(sku.getProductId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU does not belong to product");
            }
            records = inventoryChangeRecordRepository.findByProductIdAndSkuIdOrderByIdDesc(product.getId(), skuId);
        }
        return records.stream()
            .map(this::toInventoryRecordMap)
            .toList();
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

    private void applyProductCommand(Product product,
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
                                     List<MediaCommand> media) {
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setSubtitle(subtitle);
        product.setProductNo(productNo);
        product.setBasePrice(basePrice);
        product.setDescription(description);
        product.setCertificateInfo(certificateInfo);
        product.setServiceInfo(serviceInfo);
        product.setSupportCustom(Boolean.TRUE.equals(supportCustom));
        product.setHotFlag(Boolean.TRUE.equals(hotFlag));
        product.setNewFlag(Boolean.TRUE.equals(newFlag));
        product.setTagsCsv(tags == null || tags.isEmpty() ? "" : String.join(",", tags));
        product.setStatus(status);
        product.setCoverImage(media == null || media.isEmpty() ? null : media.getFirst().mediaUrl());
    }

    private void replaceProductMedia(Long productId, List<MediaCommand> mediaCommands) {
        productMediaRepository.deleteByProductId(productId);
        if (mediaCommands == null) {
            return;
        }
        for (MediaCommand mediaCommand : mediaCommands) {
            ProductMedia media = new ProductMedia();
            media.setProductId(productId);
            media.setMediaType(mediaCommand.mediaType());
            media.setMediaUrl(mediaCommand.mediaUrl());
            media.setSortOrder(mediaCommand.sortOrder());
            productMediaRepository.save(media);
        }
    }

    private void replaceProductSkus(Long productId, List<AdminSkuCommand> skuCommands) {
        if (skuCommands == null || skuCommands.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品至少需要一个 SKU");
        }
        List<ProductSku> existingSkus = productSkuRepository.findByProductIdOrderByIdAsc(productId);
        int existingIndex = 0;
        for (AdminSkuCommand skuCommand : skuCommands) {
            ProductSku sku;
            if (skuCommand.id() != null) {
                sku = productSkuRepository.findById(skuCommand.id())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在"));
                if (!productId.equals(sku.getProductId())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 不属于当前商品");
                }
            } else if (existingIndex < existingSkus.size()) {
                sku = existingSkus.get(existingIndex);
            } else {
                sku = new ProductSku();
                sku.setProductId(productId);
            }
            existingIndex++;
            sku.setProductId(productId);
            sku.setSkuCode(skuCommand.skuCode());
            sku.setMaterial(skuCommand.material());
            sku.setRingSize(skuCommand.ringSize());
            sku.setWeightDesc(skuCommand.weightDesc());
            sku.setSalePrice(skuCommand.salePrice());
            sku.setStock(skuCommand.stock());
            sku.setStatus(skuCommand.status());
            productSkuRepository.save(sku);
        }
        if (existingSkus.size() > skuCommands.size()) {
            existingSkus.stream().skip(skuCommands.size()).forEach(sku -> {
                sku.setStatus(SkuStatus.DISABLED);
                productSkuRepository.save(sku);
            });
        }
    }

    private void createProductSkus(Long productId, List<SkuCommand> skuCommands) {
        if (skuCommands == null || skuCommands.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品至少需要一个 SKU");
        }
        for (SkuCommand skuCommand : skuCommands) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSkuCode(skuCommand.skuCode());
            sku.setMaterial(skuCommand.material());
            sku.setRingSize(skuCommand.ringSize());
            sku.setWeightDesc(skuCommand.weightDesc());
            sku.setSalePrice(skuCommand.salePrice());
            sku.setStock(skuCommand.stock());
            sku.setStatus(skuCommand.status());
            productSkuRepository.save(sku);
        }
    }

    private boolean matchesKeyword(Product product, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(product.getName(), normalized)
            || containsIgnoreCase(product.getSubtitle(), normalized)
            || containsIgnoreCase(product.getProductNo(), normalized);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase(Locale.ROOT).contains(keyword);
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
        return getSkuMaps(productId, includeDisabled, DEFAULT_LOW_STOCK_THRESHOLD);
    }

    private List<Map<String, Object>> getSkuMaps(Long productId, boolean includeDisabled, int stockThreshold) {
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
                map.put("lowStock", isLowStockSku(sku, stockThreshold));
                map.put("lowStockThreshold", stockThreshold);
                return map;
            })
            .toList();
    }

    private boolean isLowStockSku(ProductSku sku, int stockThreshold) {
        return sku.getStatus() == SkuStatus.ENABLED && sku.getStock() != null && sku.getStock() <= stockThreshold;
    }

    private int normalizeStockThreshold(Integer stockThreshold) {
        return stockThreshold == null || stockThreshold < 0 ? DEFAULT_LOW_STOCK_THRESHOLD : stockThreshold;
    }

    private Map<String, Object> toInventoryRecordMap(InventoryChangeRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("productId", record.getProductId());
        map.put("skuId", record.getSkuId());
        map.put("beforeStock", record.getBeforeStock());
        map.put("afterStock", record.getAfterStock());
        map.put("changeReason", record.getChangeReason());
        map.put("operatorId", record.getOperatorId());
        map.put("operatorName", record.getOperatorName());
        map.put("createdAt", record.getCreatedAt());
        return map;
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

    public record AdminUpdateProductCommand(
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
        List<AdminSkuCommand> skus
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

    public record AdminSkuCommand(
        Long id,
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
