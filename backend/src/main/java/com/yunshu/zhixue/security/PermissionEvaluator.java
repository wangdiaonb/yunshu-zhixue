package com.yunshu.zhixue.security;

/**
 * 权限判定 SPI：由 {@code system} 模块实现（按角色查询权限码集合，可加缓存）。
 *
 * <p>{@code security} 包不依赖业务模块，未提供实现时权限注解一律拒绝（fail-closed）。</p>
 */
public interface PermissionEvaluator {

    /**
     * @param adminUserId   后台用户主键
     * @param permissionCode 权限码，如 {@code question:audit}
     * @return 是否拥有该权限点
     */
    boolean hasPermission(Long adminUserId, String permissionCode);
}
