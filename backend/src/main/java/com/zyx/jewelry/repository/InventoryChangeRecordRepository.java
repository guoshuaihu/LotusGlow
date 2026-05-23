package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.InventoryChangeRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryChangeRecordRepository extends JpaRepository<InventoryChangeRecord, Long> {

    List<InventoryChangeRecord> findBySkuIdOrderByIdDesc(Long skuId);
}
