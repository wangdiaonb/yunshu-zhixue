package com.yunshu.zhixue.modules.system.dto;

/**
 * 后台账号凭证信息：供 auth 模块登录校验与刷新令牌时使用。
 *
 * @param id           后台用户主键
 * @param username     用户名（登录账号）
 * @param passwordHash BCrypt 密码哈希
 * @param roleId       角色主键
 * @param status       1 启用 / 0 禁用
 */
public record AdminAccount(Long id, String username, String passwordHash, Long roleId, int status) {

    public boolean isEnabled() {
        return status == 1;
    }
}
