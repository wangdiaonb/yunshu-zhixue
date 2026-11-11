package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 账号密码登录入参（学员端 P-A03、管理端 A-A01 共用）。
 *
 * @param account  学员端为手机号/邮箱；管理端为用户名/手机号
 * @param password 密码 6~20 位
 */
public record AccountPasswordLoginRequest(

        @NotBlank(message = "请输入账号")
        @Size(max = 100, message = "账号长度不能超过 100 个字符")
        String account,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String password
) {
}
