package com.zyx.jewelry.auth.service;

import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import com.zyx.jewelry.common.LoginRole;
import com.zyx.jewelry.common.LoginUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.security.token-secret}")
    private String tokenSecret;

    @Value("${app.security.expire-seconds:259200}")
    private long expireSeconds;

    public String createToken(Long userId, LoginRole role, String name) {
        long expireAt = Instant.now().plusSeconds(expireSeconds).getEpochSecond();
        String payload = userId + "|" + role.name() + "|" + expireAt + "|" + sanitize(name);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    public LoginUser parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态已失效");
        }
        if (!MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), sign(parts[0]).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态校验失败");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String[] values = payload.split("\\|", 4);
        if (values.length != 4) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态格式错误");
        }
        long expireAt = Long.parseLong(values[2]);
        if (expireAt < Instant.now().getEpochSecond()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态已过期");
        }
        return new LoginUser(Long.parseLong(values[0]), LoginRole.valueOf(values[1]), values[3]);
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("token 签名失败", exception);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("|", "");
    }
}
