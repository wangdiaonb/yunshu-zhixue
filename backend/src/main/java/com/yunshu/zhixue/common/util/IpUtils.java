package com.yunshu.zhixue.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取（限流与操作日志使用）。
 *
 * <p>生产部署于 Nginx 之后，优先取 {@code X-Forwarded-For} 首个地址；
 * 仅信任网关注入的头部，直接暴露到公网时需配合 Nginx 覆盖该头部。</p>
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            int comma = ip.indexOf(',');
            return trimToMaxLength(comma > 0 ? ip.substring(0, comma) : ip);
        }
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) {
            return trimToMaxLength(ip);
        }
        return trimToMaxLength(request.getRemoteAddr());
    }

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip);
    }

    private static String trimToMaxLength(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        String trimmed = ip.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
