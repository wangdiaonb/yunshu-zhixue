package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 角色管理接口的超级管理员校验（角色管理不设权限码，仅超管可访问，D4-5）。
 */
@Component
@RequiredArgsConstructor
public class SuperAdminGuard {

    private final AdminUserService adminUserService;

    public void check() {
        if (!adminUserService.isSuperAdmin(UserContext.requireAdminId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "仅超级管理员可操作");
        }
    }
}
