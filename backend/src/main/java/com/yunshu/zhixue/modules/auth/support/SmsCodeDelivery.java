package com.yunshu.zhixue.modules.auth.support;

/**
 * 验证码发送结果：有效秒数与本次生成的验证码（仅用于开发环境回显）。
 */
public record SmsCodeDelivery(int expireSeconds, String code) {
}
