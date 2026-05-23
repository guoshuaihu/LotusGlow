package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.OrderStatus;
import com.zyx.jewelry.model.TradeOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByOrderNo(String orderNo);

    Optional<TradeOrder> findByOrderNoAndUserId(String orderNo, Long userId);

    List<TradeOrder> findByUserIdOrderByIdDesc(Long userId);

    List<TradeOrder> findByStatusOrderByIdDesc(OrderStatus status);

    boolean existsByIdAndUserId(Long id, Long userId);
}
