package com.zyx.jewelry.auth.service;

public interface WechatAuthGateway {

    WechatSession code2Session(String code);

    record WechatSession(String openId, String unionId) {
    }
}
