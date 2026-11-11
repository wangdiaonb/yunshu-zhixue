package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.security.PermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 权限判定实现：按管理员所属角色查询操作级权限码（9 组 34 项）。
 */
@Component
@RequiredArgsConstructor
public class SystemPermissionEvaluator implements PermissionEvaluator {

    private final AdminUserService adminUserService;

    @Override
    public boolean hasPermission(Long adminUserId, String permissionCode) {
        if (adminUserId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return adminUserService.listPermissionCodes(adminUserId).contains(permissionCode);
    }
}
