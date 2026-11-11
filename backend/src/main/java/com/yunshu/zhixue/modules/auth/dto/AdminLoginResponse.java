package com.yunshu.zhixue.modules.auth.dto;

import com.yunshu.zhixue.modules.system.dto.AdminSummary;

import java.util.List;

/**
 * 管理端登录响应（A-A01）。
 *
 * @param accessToken  访问令牌（TTL 2h，aud=admin）
 * @param refreshToken 刷新令牌（TTL 14d）
 * @param adminUser    管理员信息
 * @param permissions  权限码集合（9 组 34 项，前端用于菜单与按钮渲染）
 */
public record AdminLoginResponse(String accessToken, String refreshToken,
                                 AdminSummary adminUser, List<String> permissions) {
}
