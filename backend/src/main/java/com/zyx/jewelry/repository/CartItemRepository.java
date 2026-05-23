package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByIdDesc(Long userId);

    Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
}
