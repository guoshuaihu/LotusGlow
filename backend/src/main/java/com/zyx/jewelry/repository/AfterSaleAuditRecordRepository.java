package com.zyx.jewelry.repository;

import com.zyx.jewelry.model.AfterSaleAuditRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AfterSaleAuditRecordRepository extends JpaRepository<AfterSaleAuditRecord, Long> {

    List<AfterSaleAuditRecord> findByAfterSaleRequestIdOrderByIdAsc(Long afterSaleRequestId);
}
