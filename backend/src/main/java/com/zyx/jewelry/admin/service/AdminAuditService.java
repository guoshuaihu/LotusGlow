package com.zyx.jewelry.admin.service;

import com.zyx.jewelry.model.AdminAuditLog;
import com.zyx.jewelry.model.AdminUser;
import com.zyx.jewelry.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public void log(AdminUser adminUser, String actionType, String targetType, String targetId, String detail) {
        if (adminUser == null) {
            return;
        }
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAdminUserId(adminUser.getId());
        auditLog.setAdminUsername(adminUser.getUsername());
        auditLog.setActionType(actionType);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail);
        adminAuditLogRepository.save(auditLog);
    }
}
