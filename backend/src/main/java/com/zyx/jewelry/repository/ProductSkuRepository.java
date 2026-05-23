package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.ProductSku;
import com.zyx.jewelry.model.SkuStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    List<ProductSku> findByProductIdAndStatus(Long productId, SkuStatus status);

    List<ProductSku> findByProductIdOrderByIdAsc(Long productId);

    Optional<ProductSku> findByIdAndStatus(Long id, SkuStatus status);
}
