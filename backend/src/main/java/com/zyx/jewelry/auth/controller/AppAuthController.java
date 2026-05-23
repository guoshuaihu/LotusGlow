package com.zyx.jewelry.auth.controller;

import com.zyx.jewelry.auth.service.AuthService;
import com.zyx.jewelry.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AuthService authService;

    @PostMapping("/wx-login")
    public ApiResponse<Map<String, Object>> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(authService.appLogin(request.code(), request.nickname()));
    }

    @PostMapping("/bind-phone")
    public ApiResponse<Map<String, Object>> bindPhone(@Valid @RequestBody BindPhoneRequest request) {
        return ApiResponse.success(authService.bindPhone(request.phone()));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refreshToken() {
        return ApiResponse.success(authService.refreshAppToken());
    }

    public record WxLoginRequest(
        @NotBlank(message = "code 不能为空") String code,
        @NotBlank(message = "昵称不能为空") String nickname
    ) {
    }

    public record BindPhoneRequest(
        @NotBlank(message = "手机号不能为空") String phone
    ) {
    }
}
