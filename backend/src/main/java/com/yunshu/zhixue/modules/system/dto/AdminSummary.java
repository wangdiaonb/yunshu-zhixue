package com.yunshu.zhixue.modules.system.dto;

/**
 * 登录返回的后台用户信息。
 *
 * @param id       后台用户主键
 * @param username 用户名
 * @param roleId   角色主键
 * @param roleCode 角色编码（super_admin 等）
 * @param roleName 角色名称
 */
public record AdminSummary(Long id, String username, Long roleId, String roleCode, String roleName) {
}
