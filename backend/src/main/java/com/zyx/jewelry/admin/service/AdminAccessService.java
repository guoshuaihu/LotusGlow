package com.zyx.jewelry.admin.service;

import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginUser;
import com.zyx.jewelry.common.UserContextHolder;
import com.zyx.jewelry.model.AdminPermission;
import com.zyx.jewelry.model.AdminRole;
import com.zyx.jewelry.model.AdminUser;
import com.zyx.jewelry.repository.AdminUserRepository;
import java.util.EnumSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private static final Map<AdminRole, EnumSet<AdminPermission>> ROLE_PERMISSIONS = Map.of(
        AdminRole.SUPER_ADMIN, EnumSet.allOf(AdminPermission.class),
        AdminRole.PRODUCT_OPERATOR, EnumSet.of(AdminPermission.PRODUCT_MANAGE, AdminPermission.CONTENT_MANAGE),
        AdminRole.CUSTOMER_SERVICE, EnumSet.of(AdminPermission.ORDER_MANAGE, AdminPermission.AFTER_SALE_MANAGE, AdminPermission.USER_VIEW)
    );

    private final AdminUserRepository adminUserRepository;

    public AdminUser requireAdminUser() {
        LoginUser loginUser = UserContextHolder.requireAdmin();
        return adminUserRepository.findById(loginUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "管理员账号不存在"));
    }

    public AdminUser requirePermission(AdminPermission permission) {
        AdminUser adminUser = requireAdminUser();
        AdminRole adminRole = AdminRole.valueOf(adminUser.getRoleName());
        if (!ROLE_PERMISSIONS.getOrDefault(adminRole, EnumSet.noneOf(AdminPermission.class)).contains(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号没有对应操作权限");
        }
        return adminUser;
    }

    public EnumSet<AdminPermission> listPermissions(AdminUser adminUser) {
        AdminRole adminRole = AdminRole.valueOf(adminUser.getRoleName());
        return ROLE_PERMISSIONS.getOrDefault(adminRole, EnumSet.noneOf(AdminPermission.class));
    }
}
