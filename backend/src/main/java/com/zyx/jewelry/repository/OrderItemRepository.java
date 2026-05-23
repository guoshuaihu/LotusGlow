package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    java.util.Optional<OrderItem> findByIdAndOrderId(Long id, Long orderId);
}
