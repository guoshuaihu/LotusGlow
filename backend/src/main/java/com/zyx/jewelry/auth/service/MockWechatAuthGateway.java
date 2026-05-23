package com.zyx.jewelry.auth.service;

import com.zyx.jewelry.common.BusinessException;
import com.zyx.jewelry.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MockWechatAuthGateway implements WechatAuthGateway {

    @Override
    public WechatSession code2Session(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "微信登录 code 不能为空");
        }
        String normalized = code.trim();
        return new WechatSession(normalized, "union-" + normalized);
    }
}
