package com.zyx.jewelry.config;

import com.zyx.jewelry.model.AdminRole;
import com.zyx.jewelry.model.AdminUser;
import com.zyx.jewelry.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class V2SeedDataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;

    @Override
    public void run(String... args) {
        ensureAdmin("admin", "Admin@123", "珠宝运营管理员", AdminRole.SUPER_ADMIN);
        ensureAdmin("merch", "Merch@123", "商品运营", AdminRole.PRODUCT_OPERATOR);
        ensureAdmin("service", "Service@123", "订单客服", AdminRole.CUSTOMER_SERVICE);
    }

    private void ensureAdmin(String username, String password, String displayName, AdminRole role) {
        AdminUser adminUser = adminUserRepository.findByUsername(username).orElseGet(AdminUser::new);
        adminUser.setUsername(username);
        adminUser.setPassword(password);
        adminUser.setDisplayName(displayName);
        adminUser.setRoleName(role.name());
        adminUser.setEnabled(true);
        adminUserRepository.save(adminUser);
    }
}
