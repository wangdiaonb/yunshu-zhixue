package com.yunshu.zhixue.modules.auth.dto;

import com.yunshu.zhixue.modules.student.dto.StudentSummary;

/**
 * 学员端登录响应（P-A03 / P-A10 同构）。
 *
 * @param accessToken  访问令牌（TTL 2h）
 * @param refreshToken 刷新令牌（TTL 14d）
 * @param student      学员基本信息
 */
public record PortalLoginResponse(String accessToken, String refreshToken, StudentSummary student) {
}
