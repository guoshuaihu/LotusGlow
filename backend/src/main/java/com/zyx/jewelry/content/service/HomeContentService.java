package com.zyx.jewelry.content.service;

import com.zyx.jewelry.model.Banner;
import com.zyx.jewelry.model.ContentBlock;
import com.zyx.jewelry.model.Product;
import com.zyx.jewelry.model.ProductStatus;
import com.zyx.jewelry.product.service.ProductService;
import com.zyx.jewelry.repository.BannerRepository;
import com.zyx.jewelry.repository.CategoryRepository;
import com.zyx.jewelry.repository.ContentBlockRepository;
import com.zyx.jewelry.repository.ProductRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeContentService {

    private final BannerRepository bannerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ContentBlockRepository contentBlockRepository;
    private final ProductService productService;

    public Map<String, Object> loadHomePage() {
        List<Map<String, Object>> banners = bannerRepository.findAllByOrderBySortOrderAsc().stream()
            .map(this::toBanner)
            .toList();
        List<Product> products = productRepository.findByStatus(ProductStatus.ON_SALE);
        List<Map<String, Object>> hotProducts = products.stream()
            .filter(product -> Boolean.TRUE.equals(product.getHotFlag()))
            .sorted((left, right) -> Integer.compare(right.getSalesCount(), left.getSalesCount()))
            .limit(4)
            .map(productService::toProductCard)
            .toList();
        List<Map<String, Object>> newProducts = products.stream()
            .filter(product -> Boolean.TRUE.equals(product.getNewFlag()))
            .limit(4)
            .map(productService::toProductCard)
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("banners", banners);
        response.put("categories", categoryRepository.findAllByOrderBySortOrderAsc());
        response.put("hotProducts", hotProducts);
        response.put("newProducts", newProducts);
        response.put("brandStory", findBlock("brand_story"));
        response.put("serviceNotice", findBlock("brand_service"));
        response.put("memberNotice", findBlock("home_notice"));
        return response;
    }

    private Map<String, Object> toBanner(Banner banner) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", banner.getId());
        item.put("title", banner.getTitle());
        item.put("subtitle", banner.getSubtitle());
        item.put("imageUrl", banner.getImageUrl());
        item.put("linkType", banner.getLinkType());
        item.put("linkValue", banner.getLinkValue());
        return item;
    }

    private Map<String, Object> findBlock(String key) {
        ContentBlock block = contentBlockRepository.findByBlockKey(key).orElse(null);
        if (block == null) {
            return Map.of();
        }
        return Map.of("title", block.getTitle(), "content", block.getContent());
    }
}
