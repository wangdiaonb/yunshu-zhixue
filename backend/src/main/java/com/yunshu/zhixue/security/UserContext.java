package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前用户上下文：从 Spring Security 上下文读取，请求结束由框架清理。
 */
public final class UserContext {

    private UserContext() {
    }

    /** 未登录返回 {@code null}。 */
    public static LoginUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 未登录抛出 401 + 业务码 10001。 */
    public static LoginUser getRequired() {
        LoginUser loginUser = get();
        if (loginUser == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /** 管理端操作人主键；非管理端令牌抛 403。 */
    public static Long requireAdminId() {
        LoginUser loginUser = getRequired();
        if (loginUser.audience() != TokenAudience.ADMIN) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        return loginUser.id();
    }
}
