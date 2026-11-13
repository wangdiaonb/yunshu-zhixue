package com.yunshu.zhixue.modules.auth.dto;

/**
 * 找回密码·验证通过响应（P-A05）。
 *
 * @param resetToken 一次性重置令牌，10 分钟内有效
 */
public record ResetVerifyResponse(String resetToken) {
}
