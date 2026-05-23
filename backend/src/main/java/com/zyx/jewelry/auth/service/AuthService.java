package com.zyx.jewelry.auth.service;

import com.zyx.jewelry.admin.service.AdminAccessService;
import com.zyx.jewelry.admin.service.AdminAuditService;
import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginRole;
import com.zyx.jewelry.common.UserContextHolder;
import com.zyx.jewelry.model.AdminPermission;
import com.zyx.jewelry.model.AdminUser;
import com.zyx.jewelry.model.UserProfile;
import com.zyx.jewelry.repository.AdminUserRepository;
import com.zyx.jewelry.repository.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserProfileRepository userProfileRepository;
    private final AdminUserRepository adminUserRepository;
    private final TokenService tokenService;
    private final WechatAuthGateway wechatAuthGateway;
    private final AdminAccessService adminAccessService;
    private final AdminAuditService adminAuditService;

    @Transactional
    public Map<String, Object> appLogin(String code, String nickname) {
        WechatAuthGateway.WechatSession wechatSession = wechatAuthGateway.code2Session(code);
        UserProfile userProfile = userProfileRepository.findByUnionId(wechatSession.unionId())
            .or(() -> userProfileRepository.findByOpenId(wechatSession.openId()))
            .orElseGet(UserProfile::new);
        userProfile.setOpenId(wechatSession.openId());
        userProfile.setUnionId(wechatSession.unionId());
        userProfile.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : "珠宝用户");
        userProfile.setLastLoginAt(LocalDateTime.now());
        userProfileRepository.save(userProfile);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", tokenService.createToken(userProfile.getId(), LoginRole.APP_USER, userProfile.getNickname()));
        response.put("user", toAppUser(userProfile));
        return response;
    }

    @Transactional
    public Map<String, Object> bindPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号不能为空");
        }
        UserProfile userProfile = requireCurrentUser();
        userProfile.setPhone(phone.trim());
        userProfileRepository.save(userProfile);
        return toAppUser(userProfile);
    }

    @Transactional
    public Map<String, Object> refreshAppToken() {
        UserProfile userProfile = requireCurrentUser();
        userProfile.setLastLoginAt(LocalDateTime.now());
        userProfileRepository.save(userProfile);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", tokenService.createToken(userProfile.getId(), LoginRole.APP_USER, userProfile.getNickname()));
        response.put("user", toAppUser(userProfile));
        return response;
    }

    public Map<String, Object> adminLogin(String username, String password) {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误"));
        if (!adminUser.getPassword().equals(password)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (Boolean.FALSE.equals(adminUser.getEnabled())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号已禁用");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", tokenService.createToken(adminUser.getId(), LoginRole.ADMIN, adminUser.getDisplayName()));
        response.put("username", adminUser.getUsername());
        response.put("displayName", adminUser.getDisplayName());
        response.put("roleName", adminUser.getRoleName());
        response.put("permissions", adminAccessService.listPermissions(adminUser).stream().map(AdminPermission::name).toList());
        adminAuditService.log(adminUser, "ADMIN_LOGIN", "ADMIN_USER", String.valueOf(adminUser.getId()), "管理员登录后台");
        return response;
    }

    private UserProfile requireCurrentUser() {
        Long userId = UserContextHolder.requireAppUser().id();
        return userProfileRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户信息不存在"));
    }

    private Map<String, Object> toAppUser(UserProfile userProfile) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", userProfile.getId());
        user.put("nickname", userProfile.getNickname());
        user.put("phone", userProfile.getPhone());
        user.put("avatarUrl", userProfile.getAvatarUrl());
        user.put("openId", userProfile.getOpenId());
        user.put("unionId", userProfile.getUnionId());
        user.put("lastLoginAt", userProfile.getLastLoginAt());
        return user;
    }
}
