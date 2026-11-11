package com.yunshu.zhixue.modules.student.dto;

/**
 * 学员账号凭证信息：供 auth 模块登录校验与刷新令牌时使用。
 *
 * @param id           学员主键
 * @param mobile       手机号（登录账号）
 * @param email        邮箱（可为空）
 * @param passwordHash BCrypt 密码哈希
 * @param nickname     昵称
 * @param avatar       头像 URL
 * @param status       1 已启用 / 0 已禁用
 */
public record StudentAccount(Long id, String mobile, String email, String passwordHash,
                             String nickname, String avatar, int status) {

    public boolean isEnabled() {
        return status == 1;
    }
}
