package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送短信验证码入参（P-A01）。
 *
 * @param mobile 手机号（11 位）
 * @param type   验证码用途：register / login / reset
 */
public record SmsCodeRequest(

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @NotBlank(message = "验证码用途不能为空")
        @Pattern(regexp = "register|login|reset", message = "验证码用途不正确")
        String type
) {
}
