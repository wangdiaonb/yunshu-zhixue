package com.yunshu.zhixue.security;

import lombok.Getter;

/**
 * 令牌域：两端账号体系、密钥与登录态完全独立（D12）。
 */
@Getter
public enum TokenAudience {

    PORTAL("portal"),
    ADMIN("admin");

    private final String value;

    TokenAudience(String value) {
        this.value = value;
    }

    /**
     * 按请求路径判定归属端。
     *
     * @return 非两端接口路径返回 {@code null}
     */
    public static TokenAudience ofPath(String requestUri) {
        if (requestUri == null) {
            return null;
        }
        if (requestUri.startsWith("/api/admin/")) {
            return ADMIN;
        }
        if (requestUri.startsWith("/api/portal/")) {
            return PORTAL;
        }
        return null;
    }
}
