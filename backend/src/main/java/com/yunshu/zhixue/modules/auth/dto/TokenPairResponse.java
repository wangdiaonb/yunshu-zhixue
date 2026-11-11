package com.yunshu.zhixue.modules.auth.dto;

/**
 * 刷新令牌响应（P-A07 / A-A02）。
 */
public record TokenPairResponse(String accessToken, String refreshToken) {
}
