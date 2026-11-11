package com.yunshu.zhixue.security;

import java.time.Instant;

/**
 * 当前登录用户（来自已校验的 JWT，不含权限清单）。
 *
 * @param id        账号主键
 * @param account   登录账号
 * @param audience  端标识
 * @param tokenId   当前访问令牌 jti（登出时写黑名单）
 * @param expiresAt 访问令牌过期时间（登出时据此设置黑名单 TTL）
 */
public record LoginUser(Long id, String account, TokenAudience audience, String tokenId, Instant expiresAt) {
}
