package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.Product;
import com.zyx.jewelry.model.ProductStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(ProductStatus status);
}
