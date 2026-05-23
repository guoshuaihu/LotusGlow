package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.AfterSaleRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AfterSaleRequestRepository extends JpaRepository<AfterSaleRequest, Long> {

    List<AfterSaleRequest> findByUserIdOrderByIdDesc(Long userId);

    List<AfterSaleRequest> findAllByOrderByIdDesc();
}
