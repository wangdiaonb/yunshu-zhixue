package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 学员注册入参（P-A02）；确认密码由前端校验，不入参。
 */
public record RegisterRequest(

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @NotBlank(message = "请输入验证码")
        @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
        String code,

        @NotBlank(message = "请设置密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String password
) {
}
