package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.ProductMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdOrderBySortOrderAsc(Long productId);

    void deleteByProductId(Long productId);
}
